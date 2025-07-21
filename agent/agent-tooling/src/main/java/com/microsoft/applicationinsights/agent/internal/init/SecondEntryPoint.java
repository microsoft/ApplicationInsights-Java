// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static java.util.concurrent.TimeUnit.MINUTES;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.AzureMonitorExporterProviderKeys;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.AzureMonitorLogRecordExporterProvider;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.AzureMonitorMetricExporterProvider;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.AzureMonitorSpanExporterProvider;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.LogDataMapper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.MetricDataMapper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.SpanDataMapper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.configuration.ConnectionString;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.heartbeat.HeartbeatExporter;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.QuickPulse;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.statsbeat.Feature;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.statsbeat.StatsbeatModule;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.AzureMonitorHelper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.PropertyHelper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.Strings;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.TempDirs;
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
import com.microsoft.applicationinsights.agent.internal.configuration.SnippetConfiguration;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentLogExporter;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentMetricExporter;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentSpanExporter;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.AiLegacyHeaderSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithLogProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.LogExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.SpanExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.AlertTriggerSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.sampling.AiFixedPercentageSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingOverrides;
import com.microsoft.applicationinsights.agent.internal.telemetry.BatchItemProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.MetricFilter;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.internal.view.AiViewRegistry;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SecondEntryPoint
    implements AutoConfigurationCustomizerProvider, AutoConfigureListener {

  private static final ClientLogger startupLogger =
      new ClientLogger("com.microsoft.applicationinsights.agent");
  private static File tempDir;

  @Nullable private static AzureMonitorLogFilteringProcessor logFilteringProcessor;

  static File getTempDir() {
    return tempDir;
  }

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    tempDir =
        TempDirs.getApplicationInsightsTempDir(
            startupLogger,
            "Telemetry will not be stored to disk and retried later"
                + " on sporadic network failures");

    Configuration configuration = FirstEntryPoint.getConfiguration();
    if (Strings.isNullOrEmpty(configuration.connectionString)) {
      if (!configuration.connectionStringConfiguredAtRuntime) {
        throw new FriendlyException(
            "No connection string provided",
            "Please provide connection string: https://go.microsoft.com/fwlink/?linkid=2153358");
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

    StatsbeatModule statsbeatModule =
        new StatsbeatModule(PropertyHelper::lazyUpdateVmRpIntegration);
    TelemetryClient telemetryClient =
        TelemetryClient.builder()
            .setCustomDimensions(configuration.customDimensions)
            .setMetricFilters(metricFilters)
            .setStatsbeatModule(statsbeatModule)
            .setTempDir(tempDir)
            .setGeneralExportQueueSize(configuration.preview.generalExportQueueCapacity)
            .setMetricsExportQueueSize(configuration.preview.metricsExportQueueCapacity)
            .setAadAuthentication(configuration.authentication)
            .setConnectionStrings(configuration.connectionString)
            .setRoleName(configuration.role.name)
            .setRoleInstance(configuration.role.instance)
            .setDiskPersistenceMaxSizeMb(configuration.preview.diskPersistenceMaxSizeMb)
            .build();

    Consumer<List<TelemetryItem>> heartbeatTelemetryItemConsumer =
        telemetryItems -> {
          for (TelemetryItem telemetryItem : telemetryItems) {
            TelemetryObservers.INSTANCE
                .getObservers()
                .forEach(consumer -> consumer.accept(telemetryItem));
            telemetryClient.getMetricsBatchItemProcessor().trackAsync(telemetryItem);
          }
        };

    if (telemetryClient.getConnectionString() != null) {
      startupLogger.verbose("connection string is not null, start HeartbeatExporter");
      // interval longer than 15 minutes is not allowed since we use this data for usage telemetry
      long intervalSeconds =
          Math.min(configuration.heartbeat.intervalSeconds, MINUTES.toSeconds(15));
      HeartbeatExporter.start(
          intervalSeconds, telemetryClient::populateDefaults, heartbeatTelemetryItemConsumer);
    }

    TelemetryClient.setActive(telemetryClient);

    RuntimeConfigurator runtimeConfigurator =
        new RuntimeConfigurator(
            telemetryClient,
            () -> logFilteringProcessor,
            configuration,
            heartbeatTelemetryItemConsumer,
            tempDir);
    BytecodeUtilImpl.featureStatsbeat = statsbeatModule.getFeatureStatsbeat();
    BytecodeUtilImpl.runtimeConfigurator = runtimeConfigurator;
    BytecodeUtilImpl.connectionStringConfiguredAtRuntime =
        configuration.connectionStringConfiguredAtRuntime;

    if (ConfigurationBuilder.inAzureFunctionsConsumptionWorker()) {
      AzureFunctions.setup(
          () -> telemetryClient.getConnectionString() != null,
          new AzureFunctionsInitializer(runtimeConfigurator));
    }

    RpConfiguration rpConfiguration = FirstEntryPoint.getRpConfiguration();
    if (rpConfiguration != null) {
      RpConfigurationPolling.startPolling(
          rpConfiguration, runtimeConfigurator, System::getenv, System::getProperty);
    }

    // initialize StatsbeatModule
    if (telemetryClient.getConnectionString() != null) {
      statsbeatModule.start(
          AzureMonitorHelper.createStatsbeatTelemetryItemExporter(
              LazyHttpClient.newHttpPipeLine(null, telemetryClient::getAadAudienceWithScope),
              statsbeatModule,
              tempDir),
          telemetryClient::getStatsbeatConnectionString,
          telemetryClient::getInstrumentationKey,
          configuration.internal.statsbeat.disabledAll,
          configuration.internal.statsbeat.shortIntervalSeconds,
          configuration.internal.statsbeat.longIntervalSeconds,
          configuration.preview.statsbeat.disabled,
          initStatsbeatFeatureSet(configuration));
    }

    if (telemetryClient.getConnectionString() != null) {
      if (configuration.preview.browserSdkLoader.enabled) {
        SnippetConfiguration.initializeSnippet(configuration.connectionString);
      }
    }

    // TODO (trask) add this method to AutoConfigurationCustomizer upstream?
    ((AutoConfiguredOpenTelemetrySdkBuilder) autoConfiguration).disableShutdownHook();

    QuickPulse quickPulse;
    if (configuration.preview.liveMetrics.enabled) {
      quickPulse =
          QuickPulse.create(
              LazyHttpClient.newHttpPipeLineWithDefaultRedirect(
                  configuration.authentication, telemetryClient::getAadAudienceWithScope),
              () -> {
                ConnectionString connectionString = telemetryClient.getConnectionString();
                return connectionString == null ? null : connectionString.getLiveEndpoint();
              },
              telemetryClient::getInstrumentationKey,
              telemetryClient.getRoleName(),
              telemetryClient.getRoleInstance(),
              FirstEntryPoint.getAgentVersion());
    } else {
      quickPulse = null;
    }
    telemetryClient.setQuickPulse(quickPulse);

    autoConfiguration
        .addPropertiesSupplier(
            () -> {
              Map<String, String> props = new HashMap<>();
              props.put("otel.traces.exporter", AzureMonitorExporterProviderKeys.EXPORTER_NAME);
              props.put("otel.metrics.exporter", AzureMonitorExporterProviderKeys.EXPORTER_NAME);
              props.put("otel.logs.exporter", AzureMonitorExporterProviderKeys.EXPORTER_NAME);
              props.put(
                  AzureMonitorExporterProviderKeys.INTERNAL_USING_AZURE_MONITOR_EXPORTER_BUILDER,
                  "true");
              props.put(
                  "otel.metric.export.interval",
                  Integer.toString(configuration.metricIntervalSeconds * 1000));
              // using small interval because need to convert to TelemetryItem as soon as possible
              // to grab data for live metrics. the real batching is done at a lower level
              // (not using batch size 1 because that seems to cause poor performance on small
              // containers)
              props.put("otel.bsp.schedule.delay", "100");
              props.put("otel.blrp.schedule.delay", "100");
              return props;
            })
        .addPropertiesCustomizer(new AiConfigCustomizer())
        .addSpanExporterCustomizer(
            (spanExporter, configProperties) -> {
              if (spanExporter instanceof AzureMonitorSpanExporterProvider.MarkerSpanExporter) {
                return buildTraceExporter(configuration, telemetryClient, quickPulse);
              }
              return wrapSpanExporter(spanExporter, configuration);
            })
        .addMetricExporterCustomizer(
            (metricExporter, configProperties) -> {
              if (metricExporter
                  instanceof AzureMonitorMetricExporterProvider.MarkerMetricExporter) {
                return buildMetricExporter(configuration, telemetryClient, metricFilters);
              } else {
                return metricExporter;
              }
            })
        .addLogRecordProcessorCustomizer(
            (logRecordProcessor, configProperties) -> {
              if (logRecordProcessor instanceof BatchLogRecordProcessor) {
                return wrapBatchLogRecordProcessor(logRecordProcessor, configuration);
              }
              return logRecordProcessor;
            })
        .addLogRecordExporterCustomizer(
            (logRecordExporter, configProperties) -> {
              if (logRecordExporter
                  instanceof AzureMonitorLogRecordExporterProvider.MarkerLogRecordExporter) {
                return buildLogRecordExporter(configuration, telemetryClient, quickPulse);
              } else {
                return wrapLogExporter(logRecordExporter, configuration);
              }
            })
        .addTracerProviderCustomizer(
            (builder, otelConfig) -> configureTracing(builder, configuration))
        .addMeterProviderCustomizer(
            (builder, otelConfig) -> configureMetrics(builder, configuration));

    AiContextCustomizerHolder.setInstance(
        new AiContextCustomizer<>(
            configuration.preview.connectionStringOverrides,
            configuration.preview.roleNameOverrides));

    // needed for 2.x bridge
    autoConfiguration.addResourceCustomizer(
        (resource, configProperties) -> {
          telemetryClient.setOtelResource(resource);
          return resource;
        });
  }

  private static LogRecordProcessor wrapBatchLogRecordProcessor(
      LogRecordProcessor logRecordProcessor, Configuration configuration) {
    List<LogRecordProcessor> logRecordProcessors = getLogRecordProcessors(configuration);

    // the filtering log record processor needs to be chained on front of the batch log
    // record processor, hopefully log filtering will be better supported by
    // OpenTelemetry SDK in the future, see
    // https://github.com/open-telemetry/opentelemetry-specification/pull/4439
    logFilteringProcessor = createLogFilteringProcessor(logRecordProcessor, configuration);

    logRecordProcessors.add(logFilteringProcessor);
    return LogRecordProcessor.composite(logRecordProcessors.toArray(new LogRecordProcessor[0]));
  }

  private static AzureMonitorLogFilteringProcessor createLogFilteringProcessor(
      LogRecordProcessor logRecordProcessor, Configuration configuration) {

    List<Configuration.SamplingOverride> logSamplingOverrides =
        configuration.sampling.overrides.stream()
            .filter(override -> override.telemetryType == SamplingTelemetryType.TRACE)
            .collect(Collectors.toList());
    List<Configuration.SamplingOverride> exceptionSamplingOverrides =
        configuration.sampling.overrides.stream()
            .filter(override -> override.telemetryType == SamplingTelemetryType.EXCEPTION)
            .collect(Collectors.toList());

    return new AzureMonitorLogFilteringProcessor(
        logSamplingOverrides,
        exceptionSamplingOverrides,
        logRecordProcessor,
        configuration.instrumentation.logging.getSeverityThreshold());
  }

  private static SpanExporter buildTraceExporter(
      Configuration configuration, TelemetryClient telemetryClient, QuickPulse quickPulse) {
    List<Configuration.SamplingOverride> exceptionSamplingOverrides =
        configuration.sampling.overrides.stream()
            .filter(override -> override.telemetryType == SamplingTelemetryType.EXCEPTION)
            .collect(Collectors.toList());
    SpanExporter spanExporter =
        createSpanExporter(
            telemetryClient,
            quickPulse,
            configuration.preview.captureHttpServer4xxAsError,
            new SamplingOverrides(exceptionSamplingOverrides));

    return wrapSpanExporter(spanExporter, configuration);
  }

  private static MetricExporter buildMetricExporter(
      Configuration configuration,
      TelemetryClient telemetryClient,
      List<MetricFilter> metricFilters) {
    MetricDataMapper mapper =
        new MetricDataMapper(
            telemetryClient::populateDefaults, configuration.preview.captureHttpServer4xxAsError);
    return new AgentMetricExporter(
        metricFilters, mapper, telemetryClient.getMetricsBatchItemProcessor());
  }

  private static LogRecordExporter buildLogRecordExporter(
      Configuration configuration, TelemetryClient telemetryClient, QuickPulse quickPulse) {
    LogRecordExporter logExporter = createLogExporter(telemetryClient, quickPulse, configuration);

    return wrapLogExporter(logExporter, configuration);
  }

  private static Set<Feature> initStatsbeatFeatureSet(Configuration config) {
    Set<Feature> featureList = new HashSet<>();
    if (config.authentication.enabled) {
      featureList.add(Feature.AAD);
    }
    if (config.preview.legacyRequestIdPropagation.enabled) {
      featureList.add(Feature.LEGACY_PROPAGATION_ENABLED);
    }

    // disabled instrumentations
    if (!config.instrumentation.azureSdk.enabled) {
      featureList.add(Feature.AZURE_SDK_DISABLED);
    }
    if (!config.instrumentation.cassandra.enabled) {
      featureList.add(Feature.CASSANDRA_DISABLED);
    }
    if (!config.instrumentation.jdbc.enabled) {
      featureList.add(Feature.JDBC_DISABLED);
    }
    if (!config.instrumentation.jms.enabled) {
      featureList.add(Feature.JMS_DISABLED);
    }
    if (!config.instrumentation.kafka.enabled) {
      featureList.add(Feature.KAFKA_DISABLED);
    }
    if (!config.instrumentation.micrometer.enabled) {
      featureList.add(Feature.MICROMETER_DISABLED);
    }
    if (!config.instrumentation.mongo.enabled) {
      featureList.add(Feature.MONGO_DISABLED);
    }
    if (!config.instrumentation.quartz.enabled) {
      featureList.add(Feature.QUARTZ_DISABLED);
    }
    if (!config.instrumentation.rabbitmq.enabled) {
      featureList.add(Feature.RABBITMQ_DISABLED);
    }
    if (!config.instrumentation.redis.enabled) {
      featureList.add(Feature.REDIS_DISABLED);
    }
    if (!config.instrumentation.springScheduling.enabled) {
      featureList.add(Feature.SPRING_SCHEDULING_DISABLED);
    }

    // preview instrumentation
    if (!config.preview.instrumentation.akka.enabled) {
      featureList.add(Feature.AKKA_DISABLED);
    }
    if (!config.preview.instrumentation.apacheCamel.enabled) {
      featureList.add(Feature.APACHE_CAMEL_DISABLED);
    }
    if (config.preview.instrumentation.grizzly.enabled) {
      featureList.add(Feature.GRIZZLY_ENABLED);
    }
    if (!config.preview.instrumentation.play.enabled) {
      featureList.add(Feature.PLAY_DISABLED);
    }
    if (!config.preview.instrumentation.springIntegration.enabled) {
      featureList.add(Feature.SPRING_INTEGRATION_DISABLED);
    }
    if (!config.preview.instrumentation.vertx.enabled) {
      featureList.add(Feature.VERTX_DISABLED);
    }
    if (!config.preview.instrumentation.jaxrsAnnotations.enabled) {
      featureList.add(Feature.JAXRS_ANNOTATIONS_DISABLED);
    }
    if (!config.preview.instrumentation.pekko.enabled) {
      featureList.add(Feature.PEKKO_DISABLED);
    }
    if (config.preview.browserSdkLoader.enabled) {
      featureList.add(Feature.BROWSER_SDK_LOADER);
    }
    if (!config.sampling.overrides.isEmpty()) {
      featureList.add(Feature.SAMPLING);
    }
    if (config.preview.captureControllerSpans) {
      featureList.add(Feature.PREVIEW_CAPTURE_CONTROLLER_SPANS);
    }
    if (!config.preview.liveMetrics.enabled) {
      featureList.add(Feature.PREVIEW_LIVE_METRICS);
    }
    if (config.preview.legacyRequestIdPropagation.enabled) {
      featureList.add(Feature.PREVIEW_LEGACY_REQUEST_ID_PROPAGATION);
    }
    if (config.preview.disablePropagation) {
      featureList.add(Feature.PREVIEW_DISABLE_PROPAGATION);
    }
    if (config.preview.captureLoggingLevelAsCustomDimension) {
      featureList.add(Feature.PREVIEW_CAPTURE_LOGGING_LEVEL_AS_CUSTOM_DIMENSION);
    }
    if (config.preview.captureLogbackCodeAttributes) {
      featureList.add(Feature.PREVIEW_CAPTURE_LOGBACK_CODE_ATTRIBUTES);
    }
    if (config.preview.captureLogbackMarker) {
      featureList.add(Feature.PREVIEW_CAPTURE_LOGBACK_MARKER);
    }
    if (config.preview.captureLog4jMarker) {
      featureList.add(Feature.PREVIEW_CAPTURE_LOG4J_MARKER);
    }
    if (!config.preview.additionalPropagators.isEmpty()) {
      featureList.add(Feature.PREVIEW_ADDITIONAL_PROPAGATORS);
    }
    if (!config.preview.inheritedAttributes.isEmpty()) {
      featureList.add(Feature.PREVIEW_INHERITED_ATTRIBUTES);
    }
    if (config.preview.gcEvents.reportingLevel != null) {
      featureList.add(Feature.PREVIEW_GC_EVENTS);
    }
    if (!config.preview.connectionStringOverrides.isEmpty()) {
      featureList.add(Feature.PREVIEW_CONNECTION_STRING_OVERRIDES);
    }
    if (!config.preview.roleNameOverrides.isEmpty()) {
      featureList.add(Feature.PREVIEW_ROLE_NAME_OVERRIDES);
    }
    if (config.preview.generalExportQueueCapacity != 2048) {
      featureList.add(Feature.PREVIEW_GENERAL_EXPORT_QUEUE_CAPACITY);
    }
    if (config.preview.metricsExportQueueCapacity != 65536) {
      featureList.add(Feature.PREVIEW_METRICS_EXPORT_QUEUE_CAPACITY);
    }
    if (config.preview.diskPersistenceMaxSizeMb != 50) {
      featureList.add(Feature.PREVIEW_DISK_PERSISTENCE_MAX_SIZE_MB);
    }
    if (!config.preview.useNormalizedValueForNonNormalizedCpuPercentage) {
      featureList.add(Feature.PREVIEW_DONT_USE_NORMALIZED);
    }
    if (!config.preview.customInstrumentation.isEmpty()) {
      featureList.add(Feature.PREVIEW_CUSTOM_INSTRUMENTATION);
    }
    if (config.preview.statsbeat.disabled) {
      featureList.add(Feature.STATSBEAT_DISABLED);
    }
    if (config.preview.disablePropagation) {
      featureList.add(Feature.PROPAGATION_DISABLED);
    }
    if (!config.preview.captureHttpServer4xxAsError) {
      featureList.add(Feature.CAPTURE_HTTP_SERVER_4XX_AS_SUCCESS);
    }
    if (!config.preview.captureHttpServerHeaders.requestHeaders.isEmpty()
        || !config.preview.captureHttpServerHeaders.responseHeaders.isEmpty()) {
      featureList.add(Feature.CAPTURE_HTTP_SERVER_HEADERS);
    }
    if (!config.preview.captureHttpClientHeaders.requestHeaders.isEmpty()
        || !config.preview.captureHttpClientHeaders.responseHeaders.isEmpty()) {
      featureList.add(Feature.CAPTURE_HTTP_CLIENT_HEADERS);
    }
    if (!config.preview.processors.isEmpty()) {
      featureList.add(Feature.TELEMETRY_PROCESSOR_ENABLED);
    }
    if (config.preview.profiler.enabled) {
      featureList.add(Feature.PROFILER_ENABLED);
    }

    // customDimensions
    if (!config.customDimensions.isEmpty()) {
      featureList.add(Feature.CUSTOM_DIMENSIONS_ENABLED);
    }

    if (config.preview.captureLoggingLevelAsCustomDimension) {
      featureList.add(Feature.LOGGING_LEVEL_CUSTOM_PROPERTY_ENABLED);
    }

    return featureList;
  }

  private static SdkTracerProviderBuilder configureTracing(
      SdkTracerProviderBuilder tracerProvider, Configuration configuration) {

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

    return tracerProvider;
  }

  private static SpanExporter createSpanExporter(
      TelemetryClient telemetryClient,
      @Nullable QuickPulse quickPulse,
      boolean captureHttpServer4xxAsError,
      SamplingOverrides exceptionSamplingOverrides) {

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
            (span, event) -> {
              AiFixedPercentageSampler sampler =
                  exceptionSamplingOverrides.getOverride(event.getAttributes());
              return sampler != null
                  && sampler
                          .shouldSampleLog(
                              span.getSpanContext(),
                              span.getAttributes().get(AiSemanticAttributes.SAMPLE_RATE))
                          .getDecision()
                      == SamplingDecision.DROP;
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
  private static List<LogRecordProcessor> getLogRecordProcessors(Configuration configuration) {
    List<LogRecordProcessor> logRecordProcessors = new ArrayList<>();

    logRecordProcessors.add(new AzureMonitorLogProcessor());

    if (ConfigurationBuilder.inAzureFunctionsWorker(System::getenv)) {
      logRecordProcessors.add(new AzureFunctionsLogProcessor());
    }

    if (!configuration.preview.inheritedAttributes.isEmpty()) {
      logRecordProcessors.add(
          new InheritedAttributesLogProcessor(configuration.preview.inheritedAttributes));
    }
    // adding this even if there are no connectionStringOverrides, in order to support
    // "ai.preview.connection_string" being set programmatically on CONSUMER spans
    // (or "ai.preview.instrumentation_key" for backwards compatibility)
    logRecordProcessors.add(new InheritedConnectionStringLogProcessor());
    // adding this even if there are no roleNameOverrides, in order to support
    // "ai.preview.service_name" being set programmatically on CONSUMER spans
    logRecordProcessors.add(new InheritedRoleNameLogProcessor());

    return logRecordProcessors;
  }

  private static LogRecordExporter createLogExporter(
      TelemetryClient telemetryClient,
      @Nullable QuickPulse quickPulse,
      Configuration configuration) {

    LogDataMapper mapper =
        new LogDataMapper(
            configuration.preview.captureLoggingLevelAsCustomDimension,
            ConfigurationBuilder.inAzureFunctionsWorker(System::getenv),
            telemetryClient::populateDefaults);

    return new AgentLogExporter(mapper, quickPulse, telemetryClient.getGeneralBatchItemProcessor());
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

  private static SdkMeterProviderBuilder configureMetrics(
      SdkMeterProviderBuilder builder, Configuration configuration) {

    // drop internal OpenTelemetry SDK metrics
    drop(builder, "io.opentelemetry.sdk.trace", "queueSize");
    drop(builder, "io.opentelemetry.sdk.trace", "processedSpans");
    drop(builder, "io.opentelemetry.sdk.logs", "queueSize");
    drop(builder, "io.opentelemetry.sdk.logs", "processedLogs");

    if (configuration.internal.preAggregatedStandardMetrics.enabled) {
      AiViewRegistry.registerViews(builder);
    }
    return builder;
  }

  private static void drop(
      SdkMeterProviderBuilder builder, String meterName, String processedSpans) {
    builder.registerView(
        InstrumentSelector.builder().setMeterName(meterName).setName(processedSpans).build(),
        View.builder().setAggregation(Aggregation.drop()).build());
  }

  @Override
  public void afterAutoConfigure(OpenTelemetrySdk sdk) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> flushAll(sdk, TelemetryClient.getActive()).join(10, TimeUnit.SECONDS)));
  }

  private static CompletableResultCode flushAll(
      OpenTelemetrySdk sdk, TelemetryClient telemetryClient) {
    CompletableResultCode sdkShutdownResult = sdk.shutdown();
    CompletableResultCode overallResult = new CompletableResultCode();
    sdkShutdownResult.whenComplete(
        () -> {
          // IMPORTANT: the metric reader flush will fail if the periodic metric reader is already
          // mid-exporter
          CompletableResultCode telemetryClientResult = telemetryClient.forceFlush();
          telemetryClientResult.whenComplete(
              () -> {
                if (sdkShutdownResult.isSuccess() && telemetryClientResult.isSuccess()) {
                  overallResult.succeed();
                } else {
                  overallResult.fail();
                }
              });
        });
    return overallResult;
  }
}
