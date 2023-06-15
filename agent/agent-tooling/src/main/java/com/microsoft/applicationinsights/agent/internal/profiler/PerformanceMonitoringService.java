// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.PidFinder;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.GcReportingLevel;
import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.AlertingSubsystemInit;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadService;
import com.microsoft.applicationinsights.agent.internal.profiler.util.ServiceLoaderUtil;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngine;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngineFactory;
import com.microsoft.applicationinsights.diagnostics.appinsights.CodeOptimizerApplicationInsightFactoryJfr;
import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This holds the various components of the monitoring system:
 *
 * <ul>
 *   <li>Profiler
 *   <li>Alerting subsystem
 *   <li>Profile Upload service
 *   <li>Diagnostic engine
 * </ul>
 */
public class PerformanceMonitoringService {
  private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);
  private final String processId;
  private final String machineName;
  private final String roleName;
  private final TelemetryClient telemetryClient;
  private final Configuration.ProfilerConfiguration configuration;
  private final GcReportingLevel reportingLevel;
  private final File tempDir;

  private boolean currentlyEnabled = false;

  //////////////////////////////////////////////////////////
  // These are brought up and down on demand if the profiler is fully enabled
  @Nullable private AlertingSubsystem alerting;
  @Nullable private UploadService uploadService;
  @Nullable private ScheduledExecutorService alertServiceExecutorService;
  @Nullable private Profiler profiler;
  @Nullable private DiagnosticEngine diagnosticEngine;
  @Nullable private ScheduledExecutorService diagnosticEngineExecutorService;

  //////////////////////////////////////////////////////////

  public PerformanceMonitoringService(
      String processId,
      String machineName,
      String roleName,
      TelemetryClient telemetryClient,
      Configuration.ProfilerConfiguration configuration,
      GcReportingLevel reportingLevel,
      File tempDir) {
    this.processId = processId;
    this.machineName = machineName;
    this.roleName = roleName;
    this.telemetryClient = telemetryClient;
    this.configuration = configuration;
    this.reportingLevel = reportingLevel;
    this.tempDir = tempDir;
  }

  // When we introduce full disable, tighten up the synchronization here
  synchronized void enableProfiler(
      ServiceProfilerClient serviceProfilerClient,
      ScheduledExecutorService serviceProfilerExecutorService) {
    // Prevent double init
    if (currentlyEnabled) {
      return;
    }
    currentlyEnabled = true;
    logger.warn("INITIALISING JFR PROFILING SUBSYSTEM THIS FEATURE IS IN BETA");

    diagnosticEngine = null;
    if (configuration.enableDiagnostics) {
      // Initialise diagnostic service
      diagnosticEngine = startDiagnosticEngine();
    }

    alertServiceExecutorService =
        Executors.newScheduledThreadPool(
            2,
            ThreadPoolUtils.createDaemonThreadFactory(
                ProfilingInitializer.class, "ServiceProfilerAlertingService"));

    profiler = new Profiler(configuration, tempDir);

    alerting =
        AlertingSubsystemInit.create(
            configuration,
            reportingLevel,
            TelemetryObservers.INSTANCE,
            profiler,
            telemetryClient,
            diagnosticEngine,
            alertServiceExecutorService);

    uploadService =
        new UploadService(
            serviceProfilerClient,
            builder -> {},
            machineName,
            processId,
            telemetryClient::getAppId,
            roleName);

    try {
      // Daemon remains alive permanently due to scheduling an update
      profiler.initialize(uploadService, serviceProfilerExecutorService);

    } catch (Throwable t) {
      logger.error(
          "Failed to initialise profiler service",
          new RuntimeException(
              "Unable to obtain JFR connection, this may indicate that your JVM does not"
                  + " have JFR enabled. JFR profiling system will shutdown"));
      alertServiceExecutorService.shutdown();
    }
  }

  @Nullable
  private DiagnosticEngine startDiagnosticEngine() {
    try {
      DiagnosticEngineFactory diagnosticEngineFactory = loadDiagnosticEngineFactory();
      if (diagnosticEngineFactory != null) {
        diagnosticEngineExecutorService =
            Executors.newScheduledThreadPool(
                1, ThreadPoolUtils.createNamedDaemonThreadFactory("DiagnosisThreadPool"));

        DiagnosticEngine diagnosticEngine =
            diagnosticEngineFactory.create(diagnosticEngineExecutorService);

        if (diagnosticEngine != null) {
          diagnosticEngine.init(Integer.parseInt(new PidFinder().getValue()));
        } else {
          diagnosticEngineExecutorService.shutdown();
        }
        return diagnosticEngine;
      } else {
        logger.warn("No diagnostic engine implementation provided");
      }
    } catch (Throwable e) {
      // This is a broad catch however there could be a broad range of issues such as ClassNotFound
      // and dont want to disrupt the rest of the code
      logger.error("Failed to initialize the diagnostic engine", e);
    }
    return null;
  }

  @Nullable
  private static DiagnosticEngineFactory loadDiagnosticEngineFactory() {
    logger.info("loading DiagnosticEngineFactory");
    List<DiagnosticEngineFactory> diagnosticEngines =
        ServiceLoaderUtil.findAllServiceLoaders(DiagnosticEngineFactory.class, true);

    if (diagnosticEngines.size() > 1) {
      // A second diagnostic engine has been provided, prefer the non-default one
      diagnosticEngines =
          diagnosticEngines.stream()
              .filter(it -> !it.getClass().equals(CodeOptimizerApplicationInsightFactoryJfr.class))
              .collect(Collectors.toList());
    }

    if (diagnosticEngines.size() == 0) {
      return null;
    }

    return diagnosticEngines.get(0);
  }

  public void updateConfiguration(AlertingConfiguration alertingConfig) {
    if (alerting != null) {
      alerting.updateConfiguration(alertingConfig);
    }
  }
}
