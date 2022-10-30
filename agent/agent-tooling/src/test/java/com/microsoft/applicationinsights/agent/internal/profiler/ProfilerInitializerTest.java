// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import static com.microsoft.applicationinsights.agent.internal.perfcounter.JvmHeapMemoryUsedPerformanceCounter.HEAP_MEM_USED_PERCENTAGE;
import static com.microsoft.applicationinsights.agent.internal.perfcounter.MetricNames.TOTAL_CPU_PERCENTAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorDomain;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryEventData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.service.BlobAccessPass;
import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.AlertingSubsystemInit;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadContext;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadFinishArgs;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadListener;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadService;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.jfr.Recording;
import com.microsoft.jfr.RecordingConfiguration;
import com.microsoft.jfr.RecordingOptions;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class ProfilerInitializerTest {

  final String timeStamp = "a-timestamp";
  final String machineName = "a-machine-name";
  final String processId = "a-process-id";
  final String stampId = "a-stamp-id";
  final String jfrExtension = "jfr";

  @Test
  void endToEndAlertTriggerCpu() throws Exception {
    endToEndAlertTriggerCycle(
        false,
        MetricTelemetryBuilder.create(TOTAL_CPU_PERCENTAGE, 100.0).build(),
        telemetry -> {
          assertThat(telemetry.getProperties()).containsEntry("Source", "JFR-CPU");
          assertThat(telemetry.getMeasurements()).containsEntry("AverageCPUUsage", 100.0);
          assertThat(telemetry.getMeasurements()).containsEntry("AverageMemoryUsage", 0.0);
        });
  }

  @Test
  void endToEndAlertTriggerManual() throws Exception {
    endToEndAlertTriggerCycle(
        true,
        MetricTelemetryBuilder.create(HEAP_MEM_USED_PERCENTAGE, 0.0).build(),
        telemetry -> {
          assertThat(telemetry.getProperties()).containsEntry("Source", "JFR-MANUAL");
          assertThat(telemetry.getMeasurements()).containsEntry("AverageCPUUsage", 0.0);
          assertThat(telemetry.getMeasurements()).containsEntry("AverageMemoryUsage", 0.0);
        });
  }

  void endToEndAlertTriggerCycle(
      boolean triggerNow,
      TelemetryItem metricTelemetryItem,
      Consumer<TelemetryEventData> assertTelemetry)
      throws Exception {
    AtomicBoolean profileInvoked = new AtomicBoolean(false);
    AtomicReference<TelemetryEventData> serviceProfilerIndex = new AtomicReference<>();

    String appId = UUID.randomUUID().toString();

    ServiceProfilerClient clientV2 = stubClient(triggerNow);

    Supplier<String> appIdSupplier = () -> appId;

    UploadService uploadService = getServiceProfilerJfrUpload(clientV2, appIdSupplier);

    Profiler profiler = getJfrDaemon(profileInvoked);

    Object monitor = new Object();

    TelemetryClient client = spy(TelemetryClient.createForTest());
    doAnswer(
            invocation -> {
              TelemetryItem telemetryItem = invocation.getArgument(0);
              MonitorDomain data = telemetryItem.getData().getBaseData();
              if (data instanceof TelemetryEventData) {
                if ("ServiceProfilerIndex".equals(((TelemetryEventData) data).getName())) {
                  serviceProfilerIndex.set((TelemetryEventData) data);
                }
                synchronized (monitor) {
                  monitor.notifyAll();
                }
              }
              return null;
            })
        .when(client)
        .trackAsync(any(TelemetryItem.class));

    ScheduledExecutorService serviceProfilerExecutorService =
        Executors.newScheduledThreadPool(
            2,
            ThreadPoolUtils.createDaemonThreadFactory(
                ProfilerInitializerTest.class, "ServiceProfilerService"));

    ScheduledExecutorService alertServiceExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            ThreadPoolUtils.createDaemonThreadFactory(
                ProfilerInitializerTest.class, "ServiceProfilerAlertingService"));

    // Callback invoked when a profile has been uploaded.
    // Sends index metadata about the uploaded profile
    UploadListener uploadListener = ProfilingInitializer.sendServiceProfilerIndex(client);

    Configuration config = new Configuration();

    AtomicReference<ProfilerInitializer> service = new AtomicReference<>();
    AlertingSubsystem alertService =
        AlertingSubsystemInit.create(
            config,
            alert -> awaitReferenceSet(service).getProfiler().accept(alert, uploadListener),
            TelemetryObservers.INSTANCE,
            client,
            alertServiceExecutorService);

    service.set(

        //        LocalConfig.builder()
        //            .setConfigPollPeriod(1)
        //            .setPeriodicRecordingDuration(2)
        //            .setPeriodicRecordingInterval(3)
        //            .setServiceProfilerFrontEndPoint(new URL("http://localhost"))
        //            .setMemoryTriggeredSettings(null)
        //            .setCpuTriggeredSettings(null)
        //            .setManualTriggeredSettings(null)
        //            .setTempDirectory(new File("."))
        //            .setDiagnosticsEnabled(true)
        //            .build(),

        new ProfilerInitializer(
                config.preview.profiler,
                profiler,
                ProfilingInitializer.updateAlertingConfig(alertService),
                clientV2,
                uploadService,
                serviceProfilerExecutorService)
            .innerInitialize()
            .get());

    // Wait up to 10 seconds
    for (int i = 0; i < 100; i++) {
      TelemetryObservers.INSTANCE
          .getObservers()
          .forEach(telemetryObserver -> telemetryObserver.accept(metricTelemetryItem));

      synchronized (monitor) {
        if (serviceProfilerIndex.get() != null) {
          break;
        }
        monitor.wait(100);
      }
    }

    assertThat(profileInvoked.get()).isTrue();

    assertThat(serviceProfilerIndex.get()).isNotNull();
    assertThat(serviceProfilerIndex.get().getProperties()).containsEntry("ArtifactKind", "Profile");
    assertThat(serviceProfilerIndex.get().getProperties())
        .containsEntry("EtlFileSessionId", timeStamp);
    assertThat(serviceProfilerIndex.get().getProperties()).containsEntry("DataCube", appId);
    assertThat(serviceProfilerIndex.get().getProperties()).containsEntry("Extension", jfrExtension);
    assertThat(serviceProfilerIndex.get().getProperties())
        .containsEntry("MachineName", machineName);
    assertThat(serviceProfilerIndex.get().getProperties()).containsEntry("ProcessId", processId);
    assertThat(serviceProfilerIndex.get().getProperties()).containsEntry("StampId", stampId);
    assertTelemetry.accept(serviceProfilerIndex.get());
  }

  private static ProfilerInitializer awaitReferenceSet(
      AtomicReference<ProfilerInitializer> service) {
    // Wait for up to 10 seconds
    for (int i = 0; i < 100 && service.get() == null; i++) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return service.get();
  }

  private Profiler getJfrDaemon(AtomicBoolean profileInvoked) {

    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.configPollPeriodSeconds = 1;
    config.periodicRecordingDurationSeconds = 2;
    config.periodicRecordingIntervalSeconds = 3;
    config.serviceProfilerFrontEndPoint = "http://localhost";
    config.enableDiagnostics = true;

    return new Profiler(config, new File(".")) {
      @Override
      protected void profileAndUpload(
          AlertBreach alertBreach, Duration duration, UploadListener uploadListener) {
        profileInvoked.set(true);
        super.profileAndUpload(alertBreach, Duration.ofSeconds(1), uploadListener);
      }

      @Override
      protected File createJfrFile(Duration duration) throws IOException {
        return File.createTempFile("jfrFile", jfrExtension);
      }

      @Override
      protected Recording createRecording(
          RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration) {
        return Mockito.mock(Recording.class);
      }
    };
  }

  private UploadService getServiceProfilerJfrUpload(
      ServiceProfilerClient clientV2, Supplier<String> appIdSupplier) {
    return new UploadService(clientV2, machineName, processId, appIdSupplier, "a-role-name") {
      @Override
      protected Mono<UploadFinishArgs> performUpload(
          UploadContext uploadContext, BlobAccessPass uploadPass, File file) {
        return Mono.just(new UploadFinishArgs(stampId, timeStamp));
      }
    };
  }

  private static ServiceProfilerClient stubClient(boolean triggerNow) {
    ServiceProfilerClient mock = mock(ServiceProfilerClient.class);
    when(mock.getUploadAccess(any(UUID.class), any(String.class)))
        .thenReturn(
            Mono.just(
                new BlobAccessPass("https://localhost:99999/a-blob-uri", null, "a-sas-token")));

    String expiration = triggerNow ? "999999999999999999" : "5249157885138288517";

    when(mock.getSettings(any(Date.class)))
        .thenReturn(
            Mono.just(
                "{\"id\":\"8929ed2e-24da-4ad4-8a8b-5a5ebc03abb4\",\"lastModified\":\"2021-01-25T15:46:11"
                    + ".0900613+00:00\",\"enabledLastModified\":\"0001-01-01T00:00:00+00:00\",\"enabled\":true,\"collectionPlan\":\"--single --mode immediate --immediate-profiling-duration 120  "
                    + "--expiration "
                    + expiration
                    + " --settings-moniker a-settings-moniker\",\"cpuTriggerConfiguration\":\"--cpu-trigger-enabled true --cpu-threshold 80 "
                    + "--cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400\",\"memoryTriggerConfiguration\":\"--memory-trigger-enabled true --memory-threshold 20 "
                    + "--memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400\",\"defaultConfiguration\":\"--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration "
                    + "120\",\"geoOverride\":null}"));

    return mock;
  }
}
