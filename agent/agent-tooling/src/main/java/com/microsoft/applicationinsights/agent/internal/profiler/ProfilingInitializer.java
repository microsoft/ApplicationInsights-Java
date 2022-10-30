// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.policy.DefaultRedirectStrategy;
import com.azure.core.http.policy.RedirectPolicy;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.SdkVersionFinder;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.common.SystemInformation;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.init.AppIdSupplier;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ConfigPollingInit;
import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.AlertingSubsystemInit;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadService;
import com.microsoft.applicationinsights.agent.internal.profiler.util.ServiceLoaderUtil;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngine;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngineFactory;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service profiler main entry point, wires up the items below.
 *
 * <ul>
 *   <li>Alerting telemetry monitor subsystem
 *   <li>JFR Profiling service
 *   <li>JFR Uploader service
 * </ul>
 */
public class ProfilingInitializer {

  private static final Logger logger = LoggerFactory.getLogger(ProfilingInitializer.class);

  // TODO (trask) is this needed?
  private static final AtomicBoolean initialized = new AtomicBoolean();

  public static void initialize(
      @Nullable File tempDir,
      AppIdSupplier appIdSupplier,
      Configuration config,
      TelemetryClient telemetryClient) {

    if (tempDir == null) {
      throw new FriendlyException(
          "Profile is not supported in a read-only file system.",
          "disable profiler or use a writable file system");
    }

    if (initialized.getAndSet(true)) {
      return;
    }

    ProfilingInitializer.initialize(
        appIdSupplier::get,
        SystemInformation.getProcessId(),
        config.role.instance,
        config.role.name,
        telemetryClient,
        formApplicationInsightsUserAgent(),
        config,
        tempDir);
  }

  private static synchronized void initialize(
      Supplier<String> appIdSupplier,
      String processId,
      String machineName,
      String roleName,
      TelemetryClient telemetryClient,
      String userAgent,
      Configuration configuration,
      File tempDir) {

    // Cannot use default creator, as we need to add POST to the allowed redirects
    HttpPipeline httpPipeline =
        LazyHttpClient.newHttpPipeLine(
            telemetryClient.getAadAuthentication(),
            new RedirectPolicy(
                new DefaultRedirectStrategy(
                    3,
                    "Location",
                    new HashSet<>(
                        Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST)))));

    DiagnosticEngine diagnosticEngine = null;
    if (configuration.preview.profiler.enableDiagnostics) {
      // Initialise diagnostic service
      diagnosticEngine = startDiagnosticEngine();
    }

    ScheduledExecutorService serviceProfilerExecutorService =
        Executors.newScheduledThreadPool(
            1,
            ThreadPoolUtils.createDaemonThreadFactory(
                ProfilingInitializer.class, "ServiceProfilerService"));

    ScheduledExecutorService alertServiceExecutorService =
        Executors.newScheduledThreadPool(
            2,
            ThreadPoolUtils.createDaemonThreadFactory(
                ProfilingInitializer.class, "ServiceProfilerAlertingService"));

    AtomicReference<Profiler> profilerHolder = new AtomicReference<>();

    AlertingSubsystem alerting =
        AlertingSubsystemInit.create(
            configuration,
            TelemetryObservers.INSTANCE,
            profilerHolder,
            telemetryClient,
            diagnosticEngine,
            alertServiceExecutorService);

    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(
            getServiceProfilerFrontEndPoint(configuration.preview.profiler),
            telemetryClient.getInstrumentationKey(),
            httpPipeline,
            userAgent);

    UploadService uploadService =
        new UploadService(serviceProfilerClient, machineName, processId, appIdSupplier, roleName);

    Profiler profiler = new Profiler(configuration.preview.profiler, tempDir);

    logger.warn("INITIALISING JFR PROFILING SUBSYSTEM THIS FEATURE IS IN BETA");

    serviceProfilerExecutorService.submit(
        () -> {
          try {
            // Daemon remains alive permanently due to scheduling an update
            profiler.initialize(uploadService, serviceProfilerExecutorService);

            // Monitor service remains alive permanently due to scheduling an periodic config pull
            ConfigPollingInit.startPollingForConfigUpdates(
                serviceProfilerExecutorService,
                serviceProfilerClient,
                config -> {
                  AlertingSubsystemInit.updateAlertingConfig(alerting, config);
                  profiler.updateConfiguration(config);
                },
                configuration.preview.profiler.configPollPeriodSeconds);

            profilerHolder.set(profiler);

          } catch (Throwable t) {
            logger.error(
                "Failed to initialise profiler service",
                new RuntimeException(
                    "Unable to obtain JFR connection, this may indicate that your JVM does not"
                        + " have JFR enabled. JFR profiling system will shutdown"));
            alertServiceExecutorService.shutdown();
            serviceProfilerExecutorService.shutdown();
          }
        });
  }

  @Nullable
  private static DiagnosticEngine startDiagnosticEngine() {
    try {
      DiagnosticEngineFactory diagnosticEngineFactory = loadDiagnosticEngineFactory();
      if (diagnosticEngineFactory != null) {
        ScheduledExecutorService diagnosticEngineExecutorService =
            Executors.newScheduledThreadPool(
                1, ThreadPoolUtils.createNamedDaemonThreadFactory("DiagnosisThreadPool"));

        DiagnosticEngine diagnosticEngine =
            diagnosticEngineFactory.create(diagnosticEngineExecutorService);

        if (diagnosticEngine != null) {
          diagnosticEngine.init();
        } else {
          diagnosticEngineExecutorService.shutdown();
        }
        return diagnosticEngine;
      } else {
        logger.warn("No diagnostic engine implementation provided");
      }
    } catch (RuntimeException e) {
      logger.error("Failed to load profiler factory", e);
    }
    return null;
  }

  private static DiagnosticEngineFactory loadDiagnosticEngineFactory() {
    return ServiceLoaderUtil.findServiceLoader(DiagnosticEngineFactory.class);
  }

  // visible for testing
  static URL getServiceProfilerFrontEndPoint(Configuration.ProfilerConfiguration config) {

    // If the user has overridden their service profiler endpoint use that url
    if (config.serviceProfilerFrontEndPoint != null) {
      try {
        return new URL(config.serviceProfilerFrontEndPoint);
      } catch (MalformedURLException e) {
        throw new FriendlyException(
            "Failed to parse url: " + config.serviceProfilerFrontEndPoint,
            "Ensure that the service profiler endpoint is a valid url",
            e);
      }
    }

    return TelemetryClient.getActive().getConnectionString().getProfilerEndpoint();
  }

  private static String formApplicationInsightsUserAgent() {
    String aiVersion = SdkVersionFinder.getTheValue();
    String javaVersion = System.getProperty("java.version");
    String osName = System.getProperty("os.name");
    String arch = System.getProperty("os.arch");
    return "Microsoft-ApplicationInsights-Java-Profiler/"
        + aiVersion
        + "  (Java/"
        + javaVersion
        + "; "
        + osName
        + "; "
        + arch
        + ")";
  }

  private ProfilingInitializer() {}
}
