/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.applicationinsights.internal.profiler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorDomain;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryEventData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.TelemetryObservers;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.microsoft.applicationinsights.profiler.ProfilerService;
import com.microsoft.applicationinsights.profiler.ProfilerServiceFactory;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import com.microsoft.applicationinsights.serviceprofilerapi.JfrProfilerService;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.ArtifactAcceptedResponse;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.BlobAccessPass;
import com.microsoft.applicationinsights.serviceprofilerapi.client.uploader.UploadContext;
import com.microsoft.applicationinsights.serviceprofilerapi.client.uploader.UploadFinishArgs;
import com.microsoft.applicationinsights.serviceprofilerapi.profiler.JfrProfiler;
import com.microsoft.applicationinsights.serviceprofilerapi.upload.ServiceProfilerUploader;
import com.microsoft.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import static com.microsoft.applicationinsights.TelemetryUtil.createMetricsTelemetry;

import static com.microsoft.applicationinsights.internal.perfcounter.Constants.TOTAL_CPU_PC_METRIC_NAME;
import static com.microsoft.applicationinsights.internal.perfcounter.jvm.JvmHeapMemoryUsedPerformanceCounter.HEAP_MEM_USED_PERCENTAGE;
import static org.assertj.core.api.Assertions.assertThat;

class ProfilerServiceTest {

    final String timeStamp = "a-timestamp";
    final String machineName = "a-machine-name";
    final String processId = "a-process-id";
    final String stampId = "a-stamp-id";
    final String jfrExtension = "jfr";

    @Test
    void endToEndAlertTriggerCpu() throws Exception {
        endToEndAlertTriggerCycle(
                false,
                createMetricsTelemetry(new TelemetryClient(), TOTAL_CPU_PC_METRIC_NAME, 100.0),
                telemetry -> {
                    assertThat(telemetry.getProperties().get("Source")).isEqualTo("JFR-CPU");
                    assertThat(telemetry.getMeasurements().get("AverageCPUUsage")).isEqualTo(100.0);
                    assertThat(telemetry.getMeasurements().get("AverageMemoryUsage")).isEqualTo(0.0);
                });
    }

    @Test
    void endToEndAlertTriggerManual() throws Exception {
        endToEndAlertTriggerCycle(
                true,
                createMetricsTelemetry(new TelemetryClient(), HEAP_MEM_USED_PERCENTAGE, 0.0),
                telemetry -> {
                    assertThat(telemetry.getProperties().get("Source")).isEqualTo("JFR-MANUAL");
                    assertThat(telemetry.getMeasurements().get("AverageCPUUsage")).isEqualTo(0.0);
                    assertThat(telemetry.getMeasurements().get("AverageMemoryUsage")).isEqualTo(0.0);
                });
    }

    void endToEndAlertTriggerCycle(boolean triggerNow, TelemetryItem metricTelemetry, Consumer<TelemetryEventData> assertTelemetry) throws Exception {
        AtomicBoolean profileInvoked = new AtomicBoolean(false);
        AtomicReference<TelemetryEventData> serviceProfilerIndex = new AtomicReference<>();

        String appId = UUID.randomUUID().toString();

        ServiceProfilerClientV2 clientV2 = stubClient(triggerNow);

        Supplier<String> appIdSupplier = () -> appId;

        ServiceProfilerUploader serviceProfilerUploader = getServiceProfilerJFRUpload(clientV2, appIdSupplier);

        JfrProfiler jfrProfiler = getJfrDaemon(profileInvoked);

        Object monitor = new Object();

        TelemetryClient client = new TelemetryClient() {
            @Override
            public void trackAsync(TelemetryItem telemetry) {
                MonitorDomain data = telemetry.getData().getBaseData();
                if (data instanceof TelemetryEventData) {
                    if ("ServiceProfilerIndex".equals(((TelemetryEventData) data).getName())) {
                        serviceProfilerIndex.set((TelemetryEventData) data);
                    }
                    synchronized (monitor) {
                        monitor.notifyAll();
                    }
                }
            }
        };

        ScheduledExecutorService serviceProfilerExecutorService = Executors.newScheduledThreadPool(2,
                ThreadPoolUtils.createDaemonThreadFactory(ProfilerServiceFactory.class, "ServiceProfilerService")
        );

        ScheduledExecutorService alertServiceExecutorService = Executors.newSingleThreadScheduledExecutor(
                ThreadPoolUtils.createDaemonThreadFactory(ProfilerServiceFactory.class, "ServiceProfilerAlertingService")
        );

        AtomicReference<ProfilerService> service = new AtomicReference<>();
        AlertingSubsystem alertService = AlertingServiceFactory.create(
                alert -> awaitReferenceSet(service).getProfiler().accept(alert),
                TelemetryObservers.INSTANCE,
                client,
                alertServiceExecutorService,
                new GcEventMonitor.GcEventMonitorConfiguration(GcReportingLevel.ALL));

        service.set(new JfrProfilerService(
                () -> appId,
                new ServiceProfilerServiceConfig(
                        1,
                        2,
                        3,
                        new URL("http://localhost"),
                        true,
                        null,
                        null
                ),
                jfrProfiler,
                ProfilerServiceInitializer.updateAlertingConfig(alertService),
                ProfilerServiceInitializer.sendServiceProfilerIndex(client),
                clientV2,
                serviceProfilerUploader,
                serviceProfilerExecutorService
        )
                .initialize()
                .get());

        // Wait up to 10 seconds
        for (int i = 0; i < 100; i++) {
            TelemetryObservers
                    .INSTANCE
                    .getObservers()
                    .forEach(telemetryObserver -> telemetryObserver.accept(metricTelemetry));

            synchronized (monitor) {
                if (serviceProfilerIndex.get() != null) {
                    break;
                }
                monitor.wait(100);
            }
        }

        assertThat(profileInvoked.get()).isTrue();

        assertThat(serviceProfilerIndex.get()).isNotNull();
        assertThat(serviceProfilerIndex.get().getProperties().get("ArtifactKind")).isEqualTo("Profile");
        assertThat(serviceProfilerIndex.get().getProperties().get("EtlFileSessionId")).isEqualTo(timeStamp);
        assertThat(serviceProfilerIndex.get().getProperties().get("DataCube")).isEqualTo(appId);
        assertThat(serviceProfilerIndex.get().getProperties().get("Extension")).isEqualTo(jfrExtension);
        assertThat(serviceProfilerIndex.get().getProperties().get("MachineName")).isEqualTo(machineName);
        assertThat(serviceProfilerIndex.get().getProperties().get("ProcessId")).isEqualTo(processId);
        assertThat(serviceProfilerIndex.get().getProperties().get("StampId")).isEqualTo(stampId);
        assertTelemetry.accept(serviceProfilerIndex.get());
    }

    private ProfilerService awaitReferenceSet(AtomicReference<ProfilerService> service) {
        //Wait for up to 10 seconds
        for (int i = 0; i < 100 && service.get() == null; i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return service.get();
    }

    private JfrProfiler getJfrDaemon(AtomicBoolean profileInvoked) throws MalformedURLException {
        return new JfrProfiler(new ServiceProfilerServiceConfig(
                1,
                2,
                3,
                new URL("http://localhost"),
                false,
                null,
                null)) {
            @Override
            protected void profileAndUpload(AlertBreach alertBreach, Duration duration) {
                profileInvoked.set(true);
                Recording recording = Mockito.mock(Recording.class);
                uploadNewRecording(alertBreach, Instant.now()).accept(recording);
            }

            @Override
            protected File createJfrFile(Recording recording, Instant recordingStart, Instant recordingEnd) throws IOException {
                return File.createTempFile("jfrFile", jfrExtension);
            }
        };
    }

    private ServiceProfilerUploader getServiceProfilerJFRUpload(ServiceProfilerClientV2 clientV2, Supplier<String> appIdSupplier) {
        return new ServiceProfilerUploader(
                clientV2,
                machineName,
                processId,
                appIdSupplier,
                "a-role-name"
        ) {
            @Override
            protected Mono<UploadFinishArgs> performUpload(UploadContext uploadContext, BlobAccessPass uploadPass, File file) {
                return Mono.just(new UploadFinishArgs(stampId, timeStamp));
            }
        };
    }

    private ServiceProfilerClientV2 stubClient(boolean triggerNow) {
        return new ServiceProfilerClientV2() {
            @Override
            public BlobAccessPass getUploadAccess(UUID profileId) {
                return new BlobAccessPass("https://localhost:99999/a-blob-uri", null, "a-sas-token");
            }

            @Override
            public ArtifactAcceptedResponse reportUploadFinish(UUID profileId, String etag) throws UnsupportedCharsetException {
                return null;
            }

            @Override
            public String getSettings(Date oldTimeStamp) {
                String expiration = triggerNow ? "999999999999999999" : "5249157885138288517";

                return "{\"id\":\"8929ed2e-24da-4ad4-8a8b-5a5ebc03abb4\",\"lastModified\":\"2021-01-25T15:46:11" +
                        ".0900613+00:00\",\"enabledLastModified\":\"0001-01-01T00:00:00+00:00\",\"enabled\":true,\"collectionPlan\":\"--single --mode immediate --immediate-profiling-duration 120  " +
                        "--expiration " + expiration + " --settings-moniker a-settings-moniker\",\"cpuTriggerConfiguration\":\"--cpu-trigger-enabled true --cpu-threshold 80 " +
                        "--cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400\",\"memoryTriggerConfiguration\":\"--memory-trigger-enabled true --memory-threshold 20 " +
                        "--memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400\",\"defaultConfiguration\":\"--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration " +
                        "120\",\"geoOverride\":null}";
            }
        };
    }

}
