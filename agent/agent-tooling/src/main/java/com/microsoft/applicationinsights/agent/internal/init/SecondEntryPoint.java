// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static java.util.concurrent.TimeUnit.MINUTES;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.LogDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.MetricDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.SpanDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.ConnectionString;
import com.azure.monitor.opentelemetry.exporter.implementation.heartbeat.HeartbeatExporter;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulse;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.TempDirs;
import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.bootstrap.AzureFunctions;
import com.microsoft.applicationinsights.agent.bootstrap.preagg.AiContextCustomizerHolder;
import com.microsoft.applicationinsights.agent.internal.classicsdk.BytecodeUtilImpl;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingTelemetryType;
import com.microsoft.applicationinsights.agent.internal.configuration.ConfigurationBuilder;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfiguration;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentLogExporter;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentMetricExporter;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentSpanExporter;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.AiLegacyHeaderSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithLogProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.LogExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.MySpanData;
import com.microsoft.applicationinsights.agent.internal.processors.SpanExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.profiler.ProfilingInitializer;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.AlertTriggerSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatModule;
import com.microsoft.applicationinsights.agent.internal.telemetry.BatchItemProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.MetricFilter;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder;
import io.opentelemetry.sdk.metrics.internal.view.AiViewRegistry;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SecondEntryPoint implements AutoConfigurationCustomizerProvider {

  private static final ClientLogger startupLogger =
      new ClientLogger("com.microsoft.applicationinsights.agent");

  @Nullable public static AgentLogExporter agentLogExporter;

  @Nullable private static BatchLogRecordProcessor batchLogProcessor;
  @Nullable private static BatchSpanProcessor batchSpanProcessor;
  @Nullable private static MetricReader metricReader;

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    File tempDir =
        TempDirs.getApplicationInsightsTempDir(
            startupLogger,
            "Telemetry will not be stored to disk and retried later"
                + " on sporadic network failures");

    Configuration configuration = FirstEntryPoint.getConfiguration();
    if (Strings.isNullOrEmpty(configuration.connectionString)) {
      if (!configuration.connectionStringConfiguredAtRuntime) {
        throw new FriendlyException(
            "No connection string provided", "Please provide connection string.");
      }
    }
    // TODO (trask) should configuration validation be performed earlier?
    configuration.validate();

    if (configuration.proxy.host != null) {
      LazyHttpClient.proxyHost = configuration.proxy.host;
      LazyHttpClient.proxyPortNumber = configuration.proxy.port;
      LazyHttpClient.proxyUsername = configuration.proxy.username;
      LazyHttpClient.proxyPassword = configuration.proxy.password;
    }

    List<MetricFilter> metricFilters =
        configuration.preview.processors.stream()
            .filter(processor -> processor.type == Configuration.ProcessorType.METRIC_FILTER)
            .map(MetricFilter::new)
            .collect(Collectors.toList());

    StatsbeatModule statsbeatModule = new StatsbeatModule();
    TelemetryClient telemetryClient =
        TelemetryClient.builder()
            .setCustomDimensions(configuration.customDimensions)
            .setMetricFilters(metricFilters)
            .setStatsbeatModule(statsbeatModule)
            .setTempDir(tempDir)
            .setGeneralExportQueueSize(configuration.preview.generalExportQueueCapacity)
            .setMetricsExportQueueSize(configuration.preview.metricsExportQueueCapacity)
            .setAadAuthentication(configuration.preview.authentication)
            .setConnectionStrings(
                configuration.connectionString,
                configuration.internal.statsbeat.instrumentationKey,
                configuration.internal.statsbeat.endpoint)
            .setRoleName(configuration.role.name)
            .setRoleInstance(configuration.role.instance)
            .setDiskPersistenceMaxSizeMb(configuration.preview.diskPersistenceMaxSizeMb)
            .build();

    // interval longer than 15 minutes is not allowed since we use this data for usage telemetry
    long intervalSeconds = Math.min(configuration.heartbeat.intervalSeconds, MINUTES.toSeconds(15));
    Consumer<List<TelemetryItem>> telemetryItemsConsumer =
        telemetryItems -> {
          for (TelemetryItem telemetryItem : telemetryItems) {
            TelemetryObservers.INSTANCE
                .getObservers()
                .forEach(consumer -> consumer.accept(telemetryItem));
            telemetryClient.getMetricsBatchItemProcessor().trackAsync(telemetryItem);
          }
        };
    HeartbeatExporter.start(
        intervalSeconds, telemetryClient::populateDefaults, telemetryItemsConsumer);

    TelemetryClient.setActive(telemetryClient);

    RuntimeConfigurator runtimeConfigurator =
        new RuntimeConfigurator(telemetryClient, () -> agentLogExporter, configuration);

    if (configuration.sampling.percentage != null) {
      BytecodeUtilImpl.samplingPercentage = configuration.sampling.percentage.floatValue();
    } else {
      BytecodeUtilImpl.samplingPercentage = 100;
    }
    BytecodeUtilImpl.featureStatsbeat = statsbeatModule.getFeatureStatsbeat();
    BytecodeUtilImpl.runtimeConfigurator = runtimeConfigurator;
    BytecodeUtilImpl.connectionStringConfiguredAtRuntime =
        configuration.connectionStringConfiguredAtRuntime;

    if (configuration.preview.profiler.enabled) {
      try {
        ProfilingInitializer.initialize(tempDir, configuration, telemetryClient);
      } catch (RuntimeException e) {
        startupLogger.warning("Failed to initialize profiler", e);
      }
    }

    if (ConfigurationBuilder.inAzureFunctionsConsumptionWorker()) {
      AzureFunctions.setup(
          () -> telemetryClient.getConnectionString() != null,
          new AzureFunctionsInitializer(runtimeConfigurator));
    }

    RpConfiguration rpConfiguration = FirstEntryPoint.getRpConfiguration();
    if (rpConfiguration != null) {
      RpConfigurationPolling.startPolling(rpConfiguration, runtimeConfigurator);
    }

    // initialize StatsbeatModule
    statsbeatModule.start(telemetryClient, configuration);

    // TODO (trask) add this method to AutoConfigurationCustomizer upstream?
    ((AutoConfiguredOpenTelemetrySdkBuilder) autoConfiguration).registerShutdownHook(false);

    QuickPulse quickPulse;
    if (configuration.preview.liveMetrics.enabled) {
      quickPulse =
          QuickPulse.create(
              LazyHttpClient.newHttpPipeLineWithDefaultRedirect(
                  configuration.preview.authentication),
              () -> {
                ConnectionString connectionString = telemetryClient.getConnectionString();
                return connectionString == null ? null : connectionString.getLiveEndpoint();
              },
              telemetryClient::getInstrumentationKey,
              telemetryClient.getRoleName(),
              telemetryClient.getRoleInstance(),
              configuration.preview.useNormalizedValueForNonNormalizedCpuPercentage,
              FirstEntryPoint.getAgentVersion());
    } else {
      quickPulse = null;
    }
    telemetryClient.setQuickPulse(quickPulse);

    autoConfiguration
        .addPropertiesCustomizer(new AiConfigCustomizer())
        .addSpanExporterCustomizer(
            (spanExporter, otelConfig) -> {
              if ("none".equals(otelConfig.getString("otel.traces.exporter"))) {
                // in this case the spanExporter here is the noop spanExporter
                return spanExporter;
              } else {
                return wrapSpanExporter(spanExporter, configuration);
              }
            })
        .addTracerProviderCustomizer(
            (builder, otelConfig) ->
                configureTracing(builder, telemetryClient, quickPulse, otelConfig, configuration))
        .addLogRecordExporterCustomizer(
            (logExporter, otelConfig) -> {
              if ("none".equals(otelConfig.getString("otel.logs.exporter"))) {
                // in this case the logExporter here is the noop spanExporter
                return logExporter;
              } else {
                return wrapLogExporter(logExporter, configuration);
              }
            })
        .addLoggerProviderCustomizer(
            (builder, otelConfig) ->
                configureLogging(builder, telemetryClient, quickPulse, otelConfig, configuration))
        .addMeterProviderCustomizer(
            (builder, otelConfig) ->
                configureMetrics(metricFilters, builder, telemetryClient, configuration));

    AiContextCustomizerHolder.setInstance(
        new AiContextCustomizer<>(
            configuration.preview.connectionStringOverrides,
            configuration.preview.roleNameOverrides));

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
          // IMPORTANT: the metric reader flush will fail if the periodic metric reader is already
          // mid-exporter
          CompletableResultCode telemetryClientResult = telemetryClient.forceFlush();
          telemetryClientResult.whenComplete(
              () -> {
                if (initialResult.isSuccess() && telemetryClientResult.isSuccess()) {
                  overallResult.succeed();
                } else {
                  overallResult.fail();
                }
              });
        });
    return overallResult;
  }

  private static SdkTracerProviderBuilder configureTracing(
      SdkTracerProviderBuilder tracerProvider,
      TelemetryClient telemetryClient,
      @Nullable QuickPulse quickPulse,
      ConfigProperties otelConfig,
      Configuration configuration) {

    boolean enabled = !Strings.isNullOrEmpty(configuration.connectionString);
    RuntimeConfigurator.updatePropagation(
        !configuration.preview.disablePropagation && enabled,
        configuration.preview.additionalPropagators,
        configuration.preview.legacyRequestIdPropagation.enabled);
    RuntimeConfigurator.updateSampling(
        enabled, configuration.sampling, configuration.preview.sampling);

    tracerProvider.addSpanProcessor(new AzureMonitorSpanProcessor());
    if (!configuration.preview.inheritedAttributes.isEmpty()) {
      tracerProvider.addSpanProcessor(
          new InheritedAttributesSpanProcessor(configuration.preview.inheritedAttributes));
    }
    // adding this even if there are no connectionStringOverrides, in order to support
    // overriding connection string programmatically via Classic SDK
    tracerProvider.addSpanProcessor(new InheritedConnectionStringSpanProcessor());
    // adding this even if there are no roleNameOverrides, in order to support
    // overriding role name programmatically via Classic SDK
    tracerProvider.addSpanProcessor(new InheritedRoleNameSpanProcessor());
    if (configuration.preview.profiler.enabled
        && configuration.preview.profiler.enableRequestTriggering) {
      tracerProvider.addSpanProcessor(new AlertTriggerSpanProcessor());
    }
    // legacy span processor is used to pass legacy attributes from the context (extracted by the
    // AiLegacyPropagator) to the span attributes (since there is no way to update attributes on
    // span directly from propagator)
    if (configuration.preview.legacyRequestIdPropagation.enabled) {
      tracerProvider.addSpanProcessor(new AiLegacyHeaderSpanProcessor());
    }

    String tracesExporter = otelConfig.getString("otel.traces.exporter");
    if ("none".equals(tracesExporter)) { // "none" is the default set in AiConfigCustomizer
      SpanExporter spanExporter =
          createSpanExporter(
              telemetryClient, quickPulse, configuration.preview.captureHttpServer4xxAsError);

      spanExporter = wrapSpanExporter(spanExporter, configuration);

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
            });

    BatchItemProcessor batchItemProcessor = telemetryClient.getGeneralBatchItemProcessor();

    return new StatsbeatSpanExporter(
        new AgentSpanExporter(mapper, quickPulse, batchItemProcessor),
        telemetryClient.getStatsbeatModule());
  }

  private static SpanExporter wrapSpanExporter(
      SpanExporter spanExporter, Configuration configuration) {

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
  private static SdkLoggerProviderBuilder configureLogging(
      SdkLoggerProviderBuilder builder,
      TelemetryClient telemetryClient,
      @Nullable QuickPulse quickPulse,
      ConfigProperties otelConfig,
      Configuration configuration) {

    builder.addLogRecordProcessor(new AzureMonitorLogProcessor());

    if (ConfigurationBuilder.inAzureFunctionsWorker()) {
      builder.addLogRecordProcessor(new AzureFunctionsLogProcessor());
    }

    if (!configuration.preview.inheritedAttributes.isEmpty()) {
      builder.addLogRecordProcessor(
          new InheritedAttributesLogProcessor(configuration.preview.inheritedAttributes));
    }
    // adding this even if there are no connectionStringOverrides, in order to support
    // "ai.preview.connection_string" being set programmatically on CONSUMER spans
    // (or "ai.preview.instrumentation_key" for backwards compatibility)
    builder.addLogRecordProcessor(new InheritedConnectionStringLogProcessor());
    // adding this even if there are no roleNameOverrides, in order to support
    // "ai.preview.service_name" being set programmatically on CONSUMER spans
    builder.addLogRecordProcessor(new InheritedRoleNameLogProcessor());

    String logsExporter = otelConfig.getString("otel.logs.exporter");
    if ("none".equals(logsExporter)) { // "none" is the default set in AiConfigCustomizer
      LogRecordExporter logExporter = createLogExporter(telemetryClient, quickPulse, configuration);

      logExporter = wrapLogExporter(logExporter, configuration);

      // using BatchLogProcessor in order to get off of the application thread as soon as possible
      batchLogProcessor =
          BatchLogRecordProcessor.builder(logExporter)
              .setScheduleDelay(getBatchProcessorDelay())
              .build();

      builder.addLogRecordProcessor(batchLogProcessor);
    }

    return builder;
  }

  private static LogRecordExporter createLogExporter(
      TelemetryClient telemetryClient,
      @Nullable QuickPulse quickPulse,
      Configuration configuration) {

    LogDataMapper mapper =
        new LogDataMapper(
            configuration.preview.captureLoggingLevelAsCustomDimension,
            ConfigurationBuilder.inAzureFunctionsWorker(),
            telemetryClient::populateDefaults);

    List<Configuration.SamplingOverride> logSamplingOverrides =
        configuration.preview.sampling.overrides.stream()
            .filter(override -> override.telemetryType == SamplingTelemetryType.TRACE)
            .collect(Collectors.toList());
    List<Configuration.SamplingOverride> exceptionSamplingOverrides =
        configuration.preview.sampling.overrides.stream()
            .filter(override -> override.telemetryType == SamplingTelemetryType.EXCEPTION)
            .collect(Collectors.toList());

    agentLogExporter =
        new AgentLogExporter(
            configuration.instrumentation.logging.getSeverityThreshold(),
            logSamplingOverrides,
            exceptionSamplingOverrides,
            mapper,
            quickPulse,
            telemetryClient.getGeneralBatchItemProcessor());

    return agentLogExporter;
  }

  private static LogRecordExporter wrapLogExporter(
      LogRecordExporter logExporter, Configuration configuration) {

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
      List<MetricFilter> metricFilters,
      SdkMeterProviderBuilder builder,
      TelemetryClient telemetryClient,
      Configuration configuration) {

    MetricDataMapper mapper =
        new MetricDataMapper(
            telemetryClient::populateDefaults, configuration.preview.captureHttpServer4xxAsError);
    PeriodicMetricReaderBuilder readerBuilder =
        PeriodicMetricReader.builder(
            new AgentMetricExporter(
                metricFilters, mapper, telemetryClient.getMetricsBatchItemProcessor()));
    int intervalMillis =
        Integer.getInteger(
            "applicationinsights.testing.metric-reader-interval-millis",
            configuration.metricIntervalSeconds * 1000);
    metricReader = readerBuilder.setInterval(Duration.ofMillis(intervalMillis)).build();

    if (configuration.internal.preAggregatedStandardMetrics.enabled) {
      AiViewRegistry.registerViews(builder);
    }

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
