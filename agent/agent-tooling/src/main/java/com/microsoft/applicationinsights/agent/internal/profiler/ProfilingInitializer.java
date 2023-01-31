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
import com.microsoft.applicationinsights.agent.internal.profiler.config.ConfigPollingInit;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;
import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.AlertConfigParser;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private final AtomicBoolean currentlyEnabled = new AtomicBoolean();
  private final String processId;
  private final String machineName;
  private final String roleName;
  private final TelemetryClient telemetryClient;
  private final String userAgent;
  private final Configuration configuration;
  private final File tempDir;

  //////////////////////////////////////////////////////////
  // These remain permanently live even if the profiler is disabled within the UI
  private HttpPipeline httpPipeline;
  private ScheduledExecutorService serviceProfilerExecutorService;
  private ServiceProfilerClient serviceProfilerClient;
  //////////////////////////////////////////////////////////

  private PerformanceMonitoringService performanceMonitoringService;

  private ProfilingInitializer(
      String processId,
      String machineName,
      String roleName,
      TelemetryClient telemetryClient,
      String userAgent,
      Configuration configuration,
      File tempDir) {
    this.processId = processId;
    this.machineName = machineName;
    this.roleName = roleName;
    this.telemetryClient = telemetryClient;
    this.userAgent = userAgent;
    this.configuration = configuration;
    this.tempDir = tempDir;
  }

  public static ProfilingInitializer initialize(
      File tempDir, Configuration configuration, TelemetryClient telemetryClient) {

    ProfilingInitializer profilingInitializer =
        new ProfilingInitializer(
            SystemInformation.getProcessId(),
            configuration.role.instance,
            configuration.role.name,
            telemetryClient,
            formApplicationInsightsUserAgent(),
            configuration,
            tempDir);
    profilingInitializer.initialize();
    return profilingInitializer;
  }

  public synchronized void initialize() {
    if (tempDir == null) {
      throw new FriendlyException(
          "Profile is not supported in a read-only file system.",
          "disable profiler or use a writable file system");
    }

    if (configuration.preview.profiler.enabled) {
      performInit();
    }
  }

  private synchronized void performInit() {
    // Cannot use default creator, as we need to add POST to the allowed redirects
    httpPipeline =
        LazyHttpClient.newHttpPipeLine(
            telemetryClient.getAadAuthentication(),
            new RedirectPolicy(
                new DefaultRedirectStrategy(
                    3,
                    "Location",
                    new HashSet<>(
                        Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST)))));

    serviceProfilerExecutorService =
        Executors.newScheduledThreadPool(
            1,
            ThreadPoolUtils.createDaemonThreadFactory(
                ProfilingInitializer.class, "ServiceProfilerService"));

    serviceProfilerClient =
        new ServiceProfilerClient(
            getServiceProfilerFrontEndPoint(configuration.preview.profiler),
            telemetryClient.getInstrumentationKey(),
            httpPipeline,
            userAgent);

    // Monitor service remains alive permanently due to scheduling an periodic config pull
    ConfigPollingInit.startPollingForConfigUpdates(
        serviceProfilerExecutorService,
        serviceProfilerClient,
        this::applyConfiguration,
        configuration.preview.profiler.configPollPeriodSeconds);
  }

  synchronized void applyConfiguration(ProfilerConfiguration config) {
    if (currentlyEnabled.get() || (config.isEnabled() && config.hasBeenConfigured())) {

      AlertingConfiguration alertingConfig = AlertConfigParser.toAlertingConfig(config);

      if (alertingConfig.hasAnEnabledTrigger()) {
        if (!currentlyEnabled.getAndSet(true)) {
          enableProfiler();
        }
      }

      if (currentlyEnabled.get()) {
        if (performanceMonitoringService != null) {
          performanceMonitoringService.updateConfiguration(alertingConfig);
        }
        return;
      }
    }
    disableProfiler();
  }

  synchronized void disableProfiler() {
    // Currently not shutting down when disabled
    /*
     if (currentlyEnabled.getAndSet(false)) {
       alerting = null;
       uploadService = null;
       alertServiceExecutorService = null;
       profile = null;
     }
    */
  }

  synchronized void enableProfiler() {
    performanceMonitoringService =
        new PerformanceMonitoringService(
            processId, machineName, roleName, telemetryClient, configuration, tempDir);

    performanceMonitoringService.enableProfiler(
        serviceProfilerClient, serviceProfilerExecutorService);
  }

  private URL getServiceProfilerFrontEndPoint(Configuration.ProfilerConfiguration config) {

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

    return telemetryClient.getConnectionString().getProfilerEndpoint();
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

  public boolean isEnabled() {
    return currentlyEnabled.get();
  }
}
