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

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.SdkVersionFinder;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.common.LocalFileSystemUtils;
import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.common.SystemInformation;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
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
import com.microsoft.applicationinsights.agent.internal.telemetry.ConnectionString;
import com.microsoft.applicationinsights.agent.internal.telemetry.InvalidConnectionStringException;
import com.microsoft.applicationinsights.agent.internal.telemetry.MetricFilter;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;
import io.opentelemetry.instrumentation.api.aisdk.AiLazyConfiguration;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(AgentListener.class)
public class AiComponentInstaller implements AgentListener {

  private static final Logger startupLogger =
      LoggerFactory.getLogger("com.microsoft.applicationinsights.agent");

  // TODO move to "agent builder" and then can inject this in the constructor
  //  or convert to ByteBuddy and use ByteBuddyAgentCustomizer
  private static volatile Instrumentation instrumentation;

  public static void setInstrumentation(Instrumentation inst) {
    instrumentation = inst;
  }

  private volatile AppIdSupplier appIdSupplier;

  private static final File tempDirectory =
      new File(LocalFileSystemUtils.getTempDir(), "applicationinsights/profiles");

  @Override
  public void beforeAgent(Config config) {
    start(instrumentation);
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
  }

  @Override
  public void afterAgent(Config config) {
    // only safe now to resolve app id because SSL initialization
    // triggers loading of java.util.logging (starting with Java 8u231)
    // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized.

    if (!"java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
      // Delay registering and starting AppId retrieval until the connection string becomes
      // available for Linux Consumption Plan.
      appIdSupplier.startAppIdRetrieval();
    }
  }

  private void start(Instrumentation instrumentation) {

    String codelessSdkNamePrefix = getCodelessSdkNamePrefix();
    if (codelessSdkNamePrefix != null) {
      PropertyHelper.setSdkNamePrefix(codelessSdkNamePrefix);
    }

    File javaTmpDir = new File(System.getProperty("java.io.tmpdir"));
    File tmpDir = new File(javaTmpDir, "applicationinsights-java");
    if (!tmpDir.exists() && !tmpDir.mkdirs()) {
      throw new IllegalStateException("Could not create directory: " + tmpDir.getAbsolutePath());
    }

    Configuration config = MainEntryPoint.getConfiguration();
    if (!hasConnectionStringOrInstrumentationKey(config)) {
      if (!"java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
        throw new FriendlyException(
            "No connection string or instrumentation key provided",
            "Please provide connection string or instrumentation key.");
      }
    }
    // TODO (trask) should configuration validation be performed earlier?
    // Function to validate user provided processor configuration
    validateProcessorConfiguration(config);
    // validate authentication configuration
    config.preview.authentication.validate();

    String jbossHome = System.getenv("JBOSS_HOME");
    if (!Strings.isNullOrEmpty(jbossHome)) {
      // this is used to delay SSL initialization because SSL initialization triggers loading of
      // java.util.logging (starting with Java 8u231)
      // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized
      LazyHttpClient.safeToInitLatch = new CountDownLatch(1);
      instrumentation.addTransformer(
          new JulListeningClassFileTransformer(LazyHttpClient.safeToInitLatch));
    }

    if (config.proxy.host != null) {
      LazyHttpClient.proxyHost = config.proxy.host;
      LazyHttpClient.proxyPortNumber = config.proxy.port;
    }

    List<MetricFilter> metricFilters =
        config.preview.processors.stream()
            .filter(processor -> processor.type == Configuration.ProcessorType.METRIC_FILTER)
            .map(MetricFilter::new)
            .collect(Collectors.toList());

    TelemetryClient telemetryClient =
        new TelemetryClient(config.customDimensions, metricFilters, config.preview.authentication);
    TelemetryClientInitializer.initialize(telemetryClient, config);
    TelemetryClient.setActive(telemetryClient);

    try {
      ConnectionString.updateStatsbeatConnectionString(
          config.internal.statsbeat.instrumentationKey,
          config.internal.statsbeat.endpoint,
          telemetryClient);
    } catch (InvalidConnectionStringException ex) {
      startupLogger.warn("Statsbeat endpoint is invalid. {}", ex.getMessage());
    }

    BytecodeUtilImpl.samplingPercentage = config.sampling.percentage;

    appIdSupplier = new AppIdSupplier(telemetryClient);
    AiAppId.setSupplier(appIdSupplier);

    ProfilerServiceInitializer.initialize(
        appIdSupplier::get,
        SystemInformation.getProcessId(),
        formServiceProfilerConfig(config.preview.profiler),
        config.role.instance,
        config.role.name,
        telemetryClient,
        formApplicationInsightsUserAgent(),
        formGcEventMonitorConfiguration(config.preview.gcEvents));

    // this is for Azure Function Linux consumption plan support.
    if ("java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
      AiLazyConfiguration.setAccessor(
          new LazyConfigurationAccessor(telemetryClient, appIdSupplier));
    }

    // this is currently used by Micrometer instrumentation in addition to 2.x SDK
    BytecodeUtil.setDelegate(new BytecodeUtilImpl());
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(telemetryClient));

    RpConfiguration rpConfiguration = MainEntryPoint.getRpConfiguration();
    if (rpConfiguration != null) {
      RpConfigurationPolling.startPolling(rpConfiguration, config, telemetryClient, appIdSupplier);
    }

    // initialize StatsbeatModule
    StatsbeatModule.get().start(telemetryClient, config);
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
      ProfilerConfiguration configuration) {
    URL serviceProfilerFrontEndPoint =
        TelemetryClient.getActive().getEndpointProvider().getProfilerEndpoint();
    return new ServiceProfilerServiceConfig(
        configuration.configPollPeriodSeconds,
        configuration.periodicRecordingDurationSeconds,
        configuration.periodicRecordingIntervalSeconds,
        serviceProfilerFrontEndPoint,
        configuration.enabled,
        configuration.memoryTriggeredSettings,
        configuration.cpuTriggeredSettings,
        tempDirectory);
  }

  private static void validateProcessorConfiguration(Configuration config) {
    if (config.preview == null || config.preview.processors == null) {
      return;
    }
    for (ProcessorConfig processorConfig : config.preview.processors) {
      processorConfig.validate();
    }
  }

  @Nullable
  private static String getCodelessSdkNamePrefix() {
    if (!DiagnosticsHelper.isRpIntegration()) {
      return null;
    }
    StringBuilder sdkNamePrefix = new StringBuilder(4);
    sdkNamePrefix.append(DiagnosticsHelper.rpIntegrationChar());
    if (SystemInformation.isWindows()) {
      sdkNamePrefix.append("w");
    } else if (SystemInformation.isUnix()) {
      sdkNamePrefix.append("l");
    } else {
      startupLogger.warn("could not detect os: {}", System.getProperty("os.name"));
      sdkNamePrefix.append("u");
    }
    sdkNamePrefix.append("r_"); // "r" is for "recommended"
    return sdkNamePrefix.toString();
  }

  private static boolean hasConnectionStringOrInstrumentationKey(Configuration config) {
    return !Strings.isNullOrEmpty(config.connectionString);
  }

  private static class ShutdownHook extends Thread {
    private final TelemetryClient telemetryClient;

    public ShutdownHook(TelemetryClient telemetryClient) {
      this.telemetryClient = telemetryClient;
    }

    @Override
    public void run() {
      startupLogger.debug("running shutdown hook");
      CompletableResultCode otelFlush = OpenTelemetryConfigurer.flush();
      CompletableResultCode result = new CompletableResultCode();
      otelFlush.whenComplete(
          () -> {
            CompletableResultCode batchingClientFlush = telemetryClient.flushChannelBatcher();
            batchingClientFlush.whenComplete(
                () -> {
                  if (otelFlush.isSuccess() && batchingClientFlush.isSuccess()) {
                    result.succeed();
                  } else {
                    result.fail();
                  }
                });
          });
      result.join(5, SECONDS);
      if (result.isSuccess()) {
        startupLogger.debug("flushing telemetry on shutdown completed successfully");
      } else if (Thread.interrupted()) {
        startupLogger.debug("interrupted while flushing telemetry on shutdown");
      } else {
        startupLogger.debug(
            "flushing telemetry on shutdown has taken more than 5 seconds, shutting down anyways...");
      }
    }
  }
}
