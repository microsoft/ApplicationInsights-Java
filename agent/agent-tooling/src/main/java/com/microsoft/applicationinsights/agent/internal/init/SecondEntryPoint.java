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

import static java.util.concurrent.TimeUnit.MINUTES;

import com.azure.monitor.opentelemetry.exporter.implementation.AiOperationNameSpanProcessor;
import com.azure.monitor.opentelemetry.exporter.implementation.LogDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.MetricDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.SpanDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.ConnectionString;
import com.azure.monitor.opentelemetry.exporter.implementation.heartbeat.HeartBeatProvider;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulse;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.TempDirs;
import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.bootstrap.AiAppId;
import com.microsoft.applicationinsights.agent.bootstrap.AiLazyConfiguration;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfiguration;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentLogExporter;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentMetricExporter;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentSpanExporter;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.AiLegacyHeaderSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.legacysdk.BytecodeUtilImpl;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithLogProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.LogExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.MySpanData;
import com.microsoft.applicationinsights.agent.internal.processors.SpanExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatModule;
import com.microsoft.applicationinsights.agent.internal.telemetry.BatchItemProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.MetricFilter;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLogEmitterProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogProcessor;
import io.opentelemetry.sdk.logs.export.LogExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SecondEntryPoint implements AutoConfigurationCustomizerProvider {

  private static final Logger startupLogger =
      LoggerFactory.getLogger("com.microsoft.applicationinsights.agent");

  @Nullable public static AgentLogExporter agentLogExporter;

  @Nullable private static BatchLogProcessor batchLogProcessor;
  @Nullable private static BatchSpanProcessor batchSpanProcessor;
  @Nullable private static MetricReader metricReader;

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {

    File tempDir =
        TempDirs.getApplicationInsightsTempDir(
            startupLogger,
            "Telemetry will not be stored to disk and retried later"
                + " on sporadic network failures");

    Configuration config = FirstEntryPoint.getConfiguration();
    if (Strings.isNullOrEmpty(config.connectionString)) {
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
            .setConnectionStrings(
                config.connectionString,
                config.internal.statsbeat.instrumentationKey,
                config.internal.statsbeat.endpoint)
            .setRoleName(config.role.name)
            .setRoleInstance(config.role.instance)
            .build();

    PerformanceCounterInitializer.initialize(telemetryClient, config);

    // interval longer than 15 minutes is not allowed since we use this data for usage telemetry
    long intervalSeconds = Math.min(config.heartbeat.intervalSeconds, MINUTES.toSeconds(15));
    HeartBeatProvider.start(
        intervalSeconds, telemetryClient.getMetricsBatchItemProcessor().getTelemetryItemExporter());

    TelemetryClient.setActive(telemetryClient);

    BytecodeUtilImpl.samplingPercentage = config.sampling.percentage;

    AppIdSupplier appIdSupplier = new AppIdSupplier(telemetryClient.getConnectionString());
    AiAppId.setSupplier(appIdSupplier);

    if (config.preview.profiler.enabled) {
      ProfilingInitializer.initialize(tempDir, appIdSupplier, config, telemetryClient);
    }

    // this is for Azure Function Linux consumption plan support.
    if ("java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
      AiLazyConfiguration.setAccessor(
          new LazyConfigurationAccessor(
              telemetryClient, SecondEntryPoint.agentLogExporter, appIdSupplier));
    }

    RpConfiguration rpConfiguration = FirstEntryPoint.getRpConfiguration();
    if (rpConfiguration != null) {
      RpConfigurationPolling.startPolling(rpConfiguration, config, telemetryClient, appIdSupplier);
    }

    // initialize StatsbeatModule
    statsbeatModule.start(telemetryClient, config);

    StartAppIdRetrieval.setAppIdSupplier(appIdSupplier);

    // TODO (trask) add this method to AutoConfigurationCustomizer upstream?
    ((AutoConfiguredOpenTelemetrySdkBuilder) autoConfiguration).registerShutdownHook(false);

    QuickPulse quickPulse;
    if (config.preview.liveMetrics.enabled) {
      quickPulse =
          QuickPulse.create(
              LazyHttpClient.newHttpPipeLineWithDefaultRedirect(config.preview.authentication),
              () -> {
                ConnectionString connectionString = telemetryClient.getConnectionString();
                return connectionString == null ? null : connectionString.getLiveEndpoint();
              },
              telemetryClient::getInstrumentationKey,
              telemetryClient.getRoleName(),
              telemetryClient.getRoleInstance(),
              config.preview.useNormalizedValueForNonNormalizedCpuPercentage,
              FirstEntryPoint.getAgentVersion());
    } else {
      quickPulse = null;
    }
    telemetryClient.setQuickPulse(quickPulse);

    autoConfiguration
        .addTracerProviderCustomizer(
            (builder, configProperties) ->
                configureTracing(builder, telemetryClient, quickPulse, configProperties, config))
        .addLogEmitterProviderCustomizer(
            (builder, configProperties) ->
                configureLogging(builder, telemetryClient, quickPulse, config))
        .addMeterProviderCustomizer(
            (builder, configProperties) -> configureMetrics(builder, telemetryClient, config));

    Runtime.getRuntime()
        .addShutdownHook(new Thread(() -> flushAll(telemetryClient).join(10, TimeUnit.SECONDS)));
  }

  private static CompletableResultCode flushAll(TelemetryClient telemetryClient) {
    List<CompletableResultCode> results = new ArrayList<>();
    if (batchSpanProcessor != null) {
      results.add(batchSpanProcessor.forceFlush());
    }
    if (metricReader != null) {
      results.add(metricReader.forceFlush());
    }
    if (batchLogProcessor != null) {
      results.add(batchLogProcessor.forceFlush());
    }
    CompletableResultCode overallResult = new CompletableResultCode();
    CompletableResultCode initialResult = CompletableResultCode.ofAll(results);
    initialResult.whenComplete(
        () -> {
          if (initialResult.isSuccess()) {
            CompletableResultCode telemetryClientResult = telemetryClient.forceFlush();
            telemetryClientResult.whenComplete(
                () -> {
                  if (telemetryClientResult.isSuccess()) {
                    overallResult.succeed();
                  } else {
                    overallResult.fail();
                  }
                });
          } else {
            overallResult.fail();
          }
        });
    return overallResult;
  }

  private static SdkTracerProviderBuilder configureTracing(
      SdkTracerProviderBuilder tracerProvider,
      TelemetryClient telemetryClient,
      @Nullable QuickPulse quickPulse,
      ConfigProperties config,
      Configuration configuration) {

    tracerProvider.setSampler(DelegatingSampler.getInstance());

    if (configuration.connectionString != null) {
      if (!configuration.preview.disablePropagation) {
        DelegatingPropagator.getInstance()
            .setUpStandardDelegate(
                configuration.preview.additionalPropagators,
                configuration.preview.legacyRequestIdPropagation.enabled);
      }
      DelegatingSampler.getInstance()
          .setDelegate(Samplers.getSampler(configuration.sampling.percentage, configuration));
    } else {
      // in Azure Functions, we configure later on, once we know user has opted in to tracing
      // (note: the default for DelegatingPropagator is to not propagate anything
      // and the default for DelegatingSampler is to not sample anything)
    }

    // operation name span processor is only applied on span start, so doesn't need to be chained
    // with the batch span processor
    tracerProvider.addSpanProcessor(new AiOperationNameSpanProcessor());
    // inherited attributes span processor is only applied on span start, so doesn't need to be
    // chained with the batch span processor
    if (!configuration.preview.inheritedAttributes.isEmpty()) {
      tracerProvider.addSpanProcessor(
          new InheritedAttributesSpanProcessor(configuration.preview.inheritedAttributes));
    }
    // legacy span processor is only applied on span start, so doesn't need to be chained with the
    // batch span processor
    // it is used to pass legacy attributes from the context (extracted by the AiLegacyPropagator)
    // to the span attributes (since there is no way to update attributes on span directly from
    // propagator)
    if (configuration.preview.legacyRequestIdPropagation.enabled) {
      tracerProvider.addSpanProcessor(new AiLegacyHeaderSpanProcessor());
    }
    // instrumentation key overrides span processor is only applied on span start, so doesn't need
    // to be chained with the batch span processor
    if (!configuration.preview.instrumentationKeyOverrides.isEmpty()) {
      tracerProvider.addSpanProcessor(
          new InheritedInstrumentationKeySpanProcessor(
              configuration.preview.instrumentationKeyOverrides));
    }
    // role name overrides span processor is only applied on span start, so doesn't need
    // to be chained with the batch span processor
    if (!configuration.preview.roleNameOverrides.isEmpty()) {
      tracerProvider.addSpanProcessor(
          new InheritedRoleNameSpanProcessor(configuration.preview.roleNameOverrides));
    }

    String tracesExporter = config.getString("otel.traces.exporter");
    if ("none".equals(tracesExporter)) { // "none" is the default set in ConfigOverride
      SpanExporter spanExporter =
          createSpanExporter(
              telemetryClient,
              quickPulse,
              configuration,
              configuration.preview.captureHttpServer4xxAsError);

      // using BatchSpanProcessor in order to get off of the application thread as soon as possible
      batchSpanProcessor =
          BatchSpanProcessor.builder(spanExporter)
              .setScheduleDelay(getBatchProcessorDelay())
              .build();

      tracerProvider.addSpanProcessor(batchSpanProcessor);
    }

    return tracerProvider;
  }

  private static SpanExporter createSpanExporter(
      TelemetryClient telemetryClient,
      @Nullable QuickPulse quickPulse,
      Configuration configuration,
      boolean captureHttpServer4xxAsError) {

    SpanDataMapper mapper =
        new SpanDataMapper(
            captureHttpServer4xxAsError,
            telemetryClient::populateDefaults,
            (event, instrumentationName) -> {
              boolean lettuce51 = instrumentationName.equals("io.opentelemetry.lettuce-5.1");
              if (lettuce51 && event.getName().startsWith("redis.encode.")) {
                // special case as these are noisy and come from the underlying library itself
                return true;
              }
              boolean grpc16 = instrumentationName.equals("io.opentelemetry.grpc-1.6");
              if (grpc16 && event.getName().equals("message")) {
                // OpenTelemetry semantic conventions define semi-noisy grpc events
                // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/rpc.md#events
                //
                // we want to suppress these (at least by default)
                return true;
              }
              return false;
            },
            AiAppId::getAppId);

    BatchItemProcessor batchItemProcessor = telemetryClient.getGeneralBatchItemProcessor();

    SpanExporter spanExporter =
        new StatsbeatSpanExporter(
            new AgentSpanExporter(mapper, quickPulse, batchItemProcessor),
            telemetryClient.getStatsbeatModule());

    List<ProcessorConfig> processorConfigs = getSpanProcessorConfigs(configuration);
    // NOTE if changing the span processor to something async, flush it in the shutdown hook before
    // flushing TelemetryClient
    if (!processorConfigs.isEmpty()) {
      // Reversing the order of processors before passing it Span processor
      Collections.reverse(processorConfigs);
      for (ProcessorConfig processorConfig : processorConfigs) {
        switch (processorConfig.type) {
          case ATTRIBUTE:
            spanExporter = new SpanExporterWithAttributeProcessor(processorConfig, spanExporter);
            break;
          case SPAN:
            spanExporter = new ExporterWithSpanProcessor(processorConfig, spanExporter);
            break;
          default:
            throw new IllegalStateException(
                "Not an expected ProcessorType: " + processorConfig.type);
        }
      }

      // this is temporary until semantic attributes stabilize and we make breaking change
      // then can use java.util.functions.Predicate<Attributes>
      spanExporter = new BackCompatHttpUrlProcessor(spanExporter);
    }

    return spanExporter;
  }

  private static List<ProcessorConfig> getSpanProcessorConfigs(Configuration configuration) {
    return configuration.preview.processors.stream()
        .filter(
            processor ->
                processor.type == Configuration.ProcessorType.ATTRIBUTE
                    || processor.type == Configuration.ProcessorType.SPAN)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  // QuickPulse is injected into the logging pipeline because QuickPulse displays exception
  // telemetry and exception telemetry can be reported as either span events or as log records with
  // an exception stack traces
  private static SdkLogEmitterProviderBuilder configureLogging(
      SdkLogEmitterProviderBuilder builder,
      TelemetryClient telemetryClient,
      @Nullable QuickPulse quickPulse,
      Configuration configuration) {

    LogExporter logExporter = createLogExporter(telemetryClient, quickPulse, configuration);

    // using BatchLogProcessor in order to get off of the application thread as soon as possible
    batchLogProcessor =
        BatchLogProcessor.builder(logExporter).setScheduleDelay(getBatchProcessorDelay()).build();

    // inherited attributes log processor also handles operation name, ikey and role name attributes
    // and these all need access to Span.current(), so must be run before passing off to the
    // BatchLogProcessor
    return builder.addLogProcessor(
        new InheritedAttributesLogProcessor(
            configuration.preview.inheritedAttributes, batchLogProcessor));
  }

  private static LogExporter createLogExporter(
      TelemetryClient telemetryClient,
      @Nullable QuickPulse quickPulse,
      Configuration configuration) {

    LogDataMapper mapper =
        new LogDataMapper(
            configuration.preview.captureLoggingLevelAsCustomDimension,
            telemetryClient::populateDefaults);

    agentLogExporter =
        new AgentLogExporter(
            configuration.instrumentation.logging.getSeverity(),
            mapper,
            quickPulse,
            telemetryClient.getGeneralBatchItemProcessor());

    LogExporter logExporter = agentLogExporter;

    List<ProcessorConfig> processorConfigs = getLogProcessorConfigs(configuration);
    if (!processorConfigs.isEmpty()) {
      // Reversing the order of processors before passing it Log processor
      Collections.reverse(processorConfigs);
      for (ProcessorConfig processorConfig : processorConfigs) {
        switch (processorConfig.type) {
          case ATTRIBUTE:
            logExporter = new LogExporterWithAttributeProcessor(processorConfig, logExporter);
            break;
          case LOG:
            logExporter = new ExporterWithLogProcessor(processorConfig, logExporter);
            break;
          default:
            throw new IllegalStateException(
                "Not an expected ProcessorType: " + processorConfig.type);
        }
      }
    }
    return logExporter;
  }

  private static List<ProcessorConfig> getLogProcessorConfigs(Configuration configuration) {
    return configuration.preview.processors.stream()
        .filter(
            processor ->
                processor.type == Configuration.ProcessorType.ATTRIBUTE
                    || processor.type == Configuration.ProcessorType.LOG)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static Duration getBatchProcessorDelay() {
    String delayMillisStr = System.getenv("APPLICATIONINSIGHTS_PREVIEW_BSP_SCHEDULE_DELAY");
    if (delayMillisStr != null) {
      return Duration.ofMillis(Integer.parseInt(delayMillisStr));
    } else {
      // using small interval because need to convert to TelemetryItem as soon as possible to grab
      // data for live metrics. the real batching is done at a lower level
      // (not using batch size 1 because that seems to cause poor performance on small containers)
      return Duration.ofMillis(100);
    }
  }

  private static SdkMeterProviderBuilder configureMetrics(
      SdkMeterProviderBuilder builder,
      TelemetryClient telemetryClient,
      Configuration configuration) {

    MetricDataMapper mapper =
        new MetricDataMapper(
            telemetryClient.getInstrumentationKey(), telemetryClient::populateDefaults);
    metricReader =
        PeriodicMetricReader.builder(
                new AgentMetricExporter(mapper, telemetryClient.getMetricsBatchItemProcessor()))
            .setInterval(Duration.ofSeconds(configuration.preview.metricIntervalSeconds))
            .build();

    return builder.registerMetricReader(metricReader);
  }

  private static class BackCompatHttpUrlProcessor implements SpanExporter {

    private final SpanExporter delegate;

    private BackCompatHttpUrlProcessor(SpanExporter delegate) {
      this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
      List<SpanData> copy = new ArrayList<>();
      for (SpanData span : spans) {
        copy.add(addBackCompatHttpUrl(span));
      }
      return delegate.export(copy);
    }

    private static SpanData addBackCompatHttpUrl(SpanData span) {
      Attributes attributes = span.getAttributes();
      if (attributes.get(SemanticAttributes.HTTP_URL) != null) {
        // already has http.url
        return span;
      }
      String httpUrl = SpanDataMapper.getHttpUrlFromServerSpan(attributes);
      if (httpUrl == null) {
        return span;
      }
      AttributesBuilder builder = attributes.toBuilder();
      builder.put(SemanticAttributes.HTTP_URL, httpUrl);
      return new MySpanData(span, builder.build());
    }

    @Override
    public CompletableResultCode flush() {
      return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
      return delegate.shutdown();
    }
  }
}
