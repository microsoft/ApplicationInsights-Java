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

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.TempDirs;
import com.microsoft.applicationinsights.agent.bootstrap.AiAppId;
import com.microsoft.applicationinsights.agent.bootstrap.AiLazyConfiguration;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.SdkVersionFinder;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.common.SystemInformation;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProfilerConfiguration;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfiguration;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.legacysdk.ApplicationInsightsAppenderClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.legacysdk.BytecodeUtilImpl;
import com.microsoft.applicationinsights.agent.internal.legacysdk.DependencyTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.legacysdk.HeartBeatModuleClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.legacysdk.PerformanceCounterModuleClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.legacysdk.QuickPulseClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.legacysdk.RequestNameHandlerClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.legacysdk.RequestTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.legacysdk.TelemetryClientClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.legacysdk.WebRequestTrackingFilterClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.profiler.GcEventMonitor;
import com.microsoft.applicationinsights.agent.internal.profiler.ProfilerServiceInitializer;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatModule;
import com.microsoft.applicationinsights.agent.internal.telemetry.MetricFilter;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AiComponentInstaller {

  private static final Logger startupLogger =
      LoggerFactory.getLogger("com.microsoft.applicationinsights.agent");

  static AppIdSupplier beforeAgent() {
    AppIdSupplier appIdSupplier = start();

    Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();

    // add sdk instrumentation after ensuring Global.getTelemetryClient() will not return null
    instrumentation.addTransformer(new TelemetryClientClassFileTransformer());
    instrumentation.addTransformer(new DependencyTelemetryClassFileTransformer());
    instrumentation.addTransformer(new RequestTelemetryClassFileTransformer());
    instrumentation.addTransformer(new PerformanceCounterModuleClassFileTransformer());
    instrumentation.addTransformer(new QuickPulseClassFileTransformer());
    instrumentation.addTransformer(new HeartBeatModuleClassFileTransformer());
    instrumentation.addTransformer(new ApplicationInsightsAppenderClassFileTransformer());
    instrumentation.addTransformer(new WebRequestTrackingFilterClassFileTransformer());
    instrumentation.addTransformer(new RequestNameHandlerClassFileTransformer());
    instrumentation.addTransformer(new DuplicateAgentClassFileTransformer());

    return appIdSupplier;
  }

  private static AppIdSupplier start() {

    File tempDir =
        TempDirs.getApplicationInsightsTempDir(
            startupLogger,
            "Telemetry will not be stored to disk and retried later"
                + " on sporadic network failures");

    Configuration config = MainEntryPoint.getConfiguration();
    if (!hasConnectionString(config)) {
      if (!"java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
        throw new FriendlyException(
            "No connection string provided", "Please provide connection string.");
      }
    }
    // TODO (trask) should configuration validation be performed earlier?
    config.validate();

    if (config.proxy.host != null) {
      LazyHttpClient.proxyHost = config.proxy.host;
      LazyHttpClient.proxyPortNumber = config.proxy.port;
      LazyHttpClient.proxyUsername = config.proxy.username;
      LazyHttpClient.proxyPassword = config.proxy.password;
    }

    List<MetricFilter> metricFilters =
        config.preview.processors.stream()
            .filter(processor -> processor.type == Configuration.ProcessorType.METRIC_FILTER)
            .map(MetricFilter::new)
            .collect(Collectors.toList());

    StatsbeatModule statsbeatModule = new StatsbeatModule();
    TelemetryClient telemetryClient =
        TelemetryClient.builder()
            .setCustomDimensions(config.customDimensions)
            .setMetricFilters(metricFilters)
            .setStatsbeatModule(statsbeatModule)
            .setTempDir(tempDir)
            .setGeneralExportQueueSize(config.preview.generalExportQueueCapacity)
            .setMetricsExportQueueSize(config.preview.metricsExportQueueCapacity)
            .setAadAuthentication(config.preview.authentication)
            .build();

    TelemetryClientInitializer.initialize(telemetryClient, config);
    TelemetryClient.setActive(telemetryClient);

    BytecodeUtilImpl.samplingPercentage = config.sampling.percentage;

    AppIdSupplier appIdSupplier = new AppIdSupplier(telemetryClient.getConnectionString());
    AiAppId.setSupplier(appIdSupplier);

    if (config.preview.profiler.enabled) {
      if (tempDir == null) {
        throw new FriendlyException(
            "Profile is not supported in a read-only file system.",
            "disable profiler or use a writable file system");
      }

      ProfilerServiceInitializer.initialize(
          appIdSupplier::get,
          SystemInformation.getProcessId(),
          formServiceProfilerConfig(config.preview.profiler, tempDir),
          config.role.instance,
          config.role.name,
          telemetryClient,
          formApplicationInsightsUserAgent(),
          formGcEventMonitorConfiguration(config.preview.gcEvents));
    }

    // this is for Azure Function Linux consumption plan support.
    if ("java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
      AiLazyConfiguration.setAccessor(
          new LazyConfigurationAccessor(
              telemetryClient, OpenTelemetryConfigurer.loggerExporter, appIdSupplier));
    }

    // this is currently used by Micrometer instrumentation in addition to 2.x SDK
    BytecodeUtil.setDelegate(new BytecodeUtilImpl());

    RpConfiguration rpConfiguration = MainEntryPoint.getRpConfiguration();
    if (rpConfiguration != null) {
      RpConfigurationPolling.startPolling(rpConfiguration, config, telemetryClient, appIdSupplier);
    }

    // initialize StatsbeatModule
    statsbeatModule.start(telemetryClient, config);

    return appIdSupplier;
  }

  private static GcEventMonitor.GcEventMonitorConfiguration formGcEventMonitorConfiguration(
      Configuration.GcEventConfiguration gcEvents) {
    return new GcEventMonitor.GcEventMonitorConfiguration(gcEvents.reportingLevel);
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

  private static ServiceProfilerServiceConfig formServiceProfilerConfig(
      ProfilerConfiguration configuration, File tempDir) {
    URL serviceProfilerFrontEndPoint =
        TelemetryClient.getActive().getConnectionString().getProfilerEndpoint();
    return new ServiceProfilerServiceConfig(
        configuration.configPollPeriodSeconds,
        configuration.periodicRecordingDurationSeconds,
        configuration.periodicRecordingIntervalSeconds,
        serviceProfilerFrontEndPoint,
        configuration.memoryTriggeredSettings,
        configuration.cpuTriggeredSettings,
        TempDirs.getSubDir(tempDir, "profiles"));
  }

  private static boolean hasConnectionString(Configuration config) {
    return !Strings.isNullOrEmpty(config.connectionString);
  }

  private AiComponentInstaller() {}
}
