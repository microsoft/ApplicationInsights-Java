// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.DelegatingPropagatorProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AiConfigCustomizer implements Function<ConfigProperties, Map<String, String>> {

  @Override
  public Map<String, String> apply(ConfigProperties otelConfig) {

    Configuration configuration = FirstEntryPoint.getConfiguration();

    Map<String, String> properties = new HashMap<>();
    properties.put(
        "applicationinsights.internal.micrometer.step.millis",
        Long.toString(SECONDS.toMillis(configuration.metricIntervalSeconds)));

    enableInstrumentations(otelConfig, configuration, properties);

    // enable "io.opentelemetry.sdk.autoconfigure.internal.EnvironmentResourceProvider" only. It
    // enables all resource provider by default
    properties.put(
        "otel.java.enabled.resource.providers",
        "io.opentelemetry.sdk.autoconfigure.internal.EnvironmentResourceProvider");

    if (!configuration.preview.captureControllerSpans) {
      properties.put(
          "otel.instrumentation.common.experimental.controller-telemetry.enabled", "false");
    }
    properties.put("otel.instrumentation.common.experimental.view-telemetry.enabled", "false");
    properties.put(
        "otel.instrumentation.messaging.experimental.receive-telemetry.enabled", "false");

    setHttpHeaderConfiguration(
        properties,
        "otel.instrumentation.http.capture-headers.server.request",
        configuration.preview.captureHttpServerHeaders.requestHeaders);
    setHttpHeaderConfiguration(
        properties,
        "otel.instrumentation.http.capture-headers.server.response",
        configuration.preview.captureHttpServerHeaders.responseHeaders);
    setHttpHeaderConfiguration(
        properties,
        "otel.instrumentation.http.capture-headers.client.request",
        configuration.preview.captureHttpClientHeaders.requestHeaders);
    setHttpHeaderConfiguration(
        properties,
        "otel.instrumentation.http.capture-headers.client.response",
        configuration.preview.captureHttpClientHeaders.responseHeaders);

    // enable capturing all mdc properties
    properties.put(
        "otel.instrumentation.logback-appender.experimental.capture-mdc-attributes", "*");
    properties.put("otel.instrumentation.log4j-appender.experimental.capture-mdc-attributes", "*");
    properties.put(
        "otel.instrumentation.log4j-appender.experimental.capture-context-data-attributes", "*");
    properties.put(
        "otel.instrumentation.jboss-logmanager.experimental.capture-mdc-attributes", "*");

    properties.put(
        "otel.instrumentation.log4j-appender.experimental.capture-map-message-attributes", "true");

    // enable thread.name
    properties.put("otel.instrumentation.java-util-logging.experimental-log-attributes", "true");
    properties.put("otel.instrumentation.jboss-logmanager.experimental-log-attributes", "true");
    properties.put("otel.instrumentation.log4j-appender.experimental-log-attributes", "true");
    properties.put("otel.instrumentation.logback-appender.experimental-log-attributes", "true");

    // disable logging appender
    if (!configuration.instrumentation.logging.enabled) {
      properties.put("otel.instrumentation.logback-appender.enabled", "false");
      properties.put("otel.instrumentation.log4j-appender.enabled", "false");
      properties.put("otel.instrumentation.java-util-logging.enabled", "false");
    }

    // custom instrumentation
    if (!configuration.preview.customInstrumentation.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Configuration.CustomInstrumentation customInstrumentation :
          configuration.preview.customInstrumentation) {
        if (sb.length() > 0) {
          sb.append(';');
        }
        sb.append(customInstrumentation.className);
        sb.append('[');
        sb.append(customInstrumentation.methodName);
        sb.append(']');
      }
      properties.put("applicationinsights.internal.methods.include", sb.toString());
    }

    properties.put("otel.propagators", DelegatingPropagatorProvider.NAME);

    properties.put("otel.traces.sampler", DelegatingSamplerProvider.NAME);

    String tracesExporter = otelConfig.getString("otel.traces.exporter");
    if (tracesExporter == null) {
      // this overrides the default "otlp" so the exporter can be configured later
      properties.put("otel.traces.exporter", "none");
    }

    String metricsExporter = otelConfig.getString("otel.metrics.exporter");
    if (metricsExporter == null) {
      // this overrides the default "otlp" so the exporter can be configured later
      properties.put("otel.metrics.exporter", "none");
    }

    String logsExporter = otelConfig.getString("otel.logs.exporter");
    if (logsExporter == null) {
      // this overrides the default "otlp" so the exporter can be configured later
      properties.put("otel.logs.exporter", "none");
    }

    if (configuration.role.name != null) {
      // in case using another exporter
      properties.put("otel.service.name", configuration.role.name);
    }

    return properties;
  }

  private static void enableInstrumentations(
      ConfigProperties otelConfig, Configuration config, Map<String, String> properties) {
    properties.put("otel.instrumentation.common.default-enabled", "false");

    properties.put("otel.instrumentation.experimental.span-suppression-strategy", "client");

    properties.put("otel.instrumentation.http.prefer-forwarded-url-scheme", "true");

    // instrumentation that cannot be disabled (currently at least)

    properties.put("otel.instrumentation.ai-azure-functions.enabled", "true");
    properties.put("otel.instrumentation.ai-applicationinsights-web.enabled", "true");

    properties.put("otel.instrumentation.apache-httpasyncclient.enabled", "true");
    properties.put("otel.instrumentation.apache-httpclient.enabled", "true");
    properties.put("otel.instrumentation.async-http-client.enabled", "true");
    properties.put("otel.instrumentation.executors.enabled", "true");
    properties.put("otel.instrumentation.google-http-client.enabled", "true");
    properties.put("otel.instrumentation.grpc.enabled", "true");
    properties.put("otel.instrumentation.guava.enabled", "true");
    properties.put("otel.instrumentation.http-url-connection.enabled", "true");
    properties.put("otel.instrumentation.java-http-client.enabled", "true");
    properties.put("otel.instrumentation.java-util-logging.enabled", "true");
    properties.put("otel.instrumentation.jaxrs.enabled", "true");
    properties.put("otel.instrumentation.jaxrs-client.enabled", "true");
    properties.put("otel.instrumentation.jaxws.enabled", "true");

    properties.put("otel.instrumentation.jboss-logmanager-appender.enabled", "true");
    properties.put("otel.instrumentation.jboss-logmanager-mdc.enabled", "true");
    properties.put("otel.instrumentation.jetty.enabled", "true");
    properties.put("otel.instrumentation.jetty-httpclient.enabled", "true");
    properties.put("otel.instrumentation.kotlinx-coroutines.enabled", "true");
    properties.put("otel.instrumentation.liberty.enabled", "true");
    properties.put("otel.instrumentation.liberty-dispatcher.enabled", "true");
    properties.put("otel.instrumentation.log4j-appender.enabled", "true");
    if (otelConfig.getBoolean("otel.instrumentation.logback-appender.enabled", true)) {
      properties.put("otel.instrumentation.logback-appender.enabled", "true");
    }
    properties.put("otel.instrumentation.log4j-mdc.enabled", "true");
    properties.put("otel.instrumentation.log4j-context-data.enabled", "true");
    properties.put("otel.instrumentation.logback-mdc.enabled", "true");
    properties.put("otel.instrumentation.ai-methods.enabled", "true");

    // not supporting netty-3.8 for now
    properties.put("otel.instrumentation.netty-4.0.enabled", "true");
    properties.put("otel.instrumentation.netty-4.1.enabled", "true");

    properties.put("otel.instrumentation.okhttp.enabled", "true");
    properties.put("otel.instrumentation.opentelemetry-extension-annotations.enabled", "true");
    properties.put(
        "otel.instrumentation.opentelemetry-instrumentation-annotations.enabled", "true");
    properties.put("otel.instrumentation.opentelemetry-api.enabled", "true");
    properties.put("otel.instrumentation.opentelemetry-instrumentation-api.enabled", "true");
    if (otelConfig.getBoolean("otel.instrumentation.reactor.enabled", true)
        && !"false".equalsIgnoreCase(System.getenv("OTEL_INSTRUMENTATION_REACTOR_ENABLED"))) {
      properties.put("otel.instrumentation.reactor.enabled", "true");
    }

    if (otelConfig.getBoolean("otel.instrumentation.reactor-netty.enabled", true)
        && !"false".equalsIgnoreCase(System.getenv("OTEL_INSTRUMENTATION_REACTOR_NETTY_ENABLED"))) {
      properties.put("otel.instrumentation.reactor-netty.enabled", "true");
    }
    properties.put("otel.instrumentation.rxjava.enabled", "true");

    properties.put("otel.instrumentation.servlet.enabled", "true");
    properties.put("otel.instrumentation.spring-core.enabled", "true");
    properties.put("otel.instrumentation.spring-web.enabled", "true");
    properties.put("otel.instrumentation.spring-webmvc.enabled", "true");
    properties.put("otel.instrumentation.spring-webflux.enabled", "true");
    properties.put("otel.instrumentation.tomcat.enabled", "true");
    properties.put("otel.instrumentation.undertow.enabled", "true");

    if (config.instrumentation.micrometer.enabled) {
      // TODO (heya) replace with below when updating to upstream micrometer
      properties.put("otel.instrumentation.ai-micrometer.enabled", "true");
      properties.put("otel.instrumentation.ai-actuator-metrics.enabled", "true");
      // properties.put("otel.instrumentation.micrometer.enabled", "true");
      // properties.put("otel.instrumentation.spring-boot-actuator-autoconfigure.enabled", "true");
    }
    String namespace = config.instrumentation.micrometer.namespace;
    if (namespace != null) {
      properties.put("applicationinsights.internal.micrometer.namespace", namespace);
    }
    if (config.instrumentation.azureSdk.enabled) {
      properties.put("otel.instrumentation.azure-core.enabled", "true");
    }
    if (config.instrumentation.cassandra.enabled) {
      properties.put("otel.instrumentation.cassandra.enabled", "true");
    }
    if (config.instrumentation.jdbc.enabled) {
      properties.put("otel.instrumentation.jdbc.enabled", "true");
      if (!config.instrumentation.jdbc.masking.enabled) {
        properties.put("otel.instrumentation.jdbc.statement-sanitizer.enabled", "false");
      }
    }
    if (config.instrumentation.jms.enabled) {
      properties.put("otel.instrumentation.jms.enabled", "true");
      properties.put("otel.instrumentation.spring-jms.enabled", "true");
    }
    if (config.instrumentation.kafka.enabled) {
      properties.put("otel.instrumentation.kafka.enabled", "true");
      properties.put("otel.instrumentation.spring-kafka.enabled", "true");
      // this is needed to capture kafka.record.queue_time_ms
      properties.put("otel.instrumentation.kafka.experimental-span-attributes", "true");
      // kafka metrics are enabled by default
      properties.put("otel.instrumentation.kafka.metric-reporter.enabled", "false");
    }
    if (config.instrumentation.mongo.enabled) {
      properties.put("otel.instrumentation.mongo.enabled", "true");
      if (!config.instrumentation.mongo.masking.enabled) {
        properties.put("otel.instrumentation.mongo.statement-sanitizer.enabled", "false");
      }
    }
    if (config.instrumentation.quartz.enabled) {
      properties.put("otel.instrumentation.quartz.enabled", "true");
      // this is needed for the job.system attribute in order to map those spans to requests
      properties.put("otel.instrumentation.quartz.experimental-span-attributes", "true");
    }
    if (config.instrumentation.rabbitmq.enabled) {
      properties.put("otel.instrumentation.rabbitmq.enabled", "true");
      properties.put("otel.instrumentation.spring-rabbit.enabled", "true");
    }
    if (config.instrumentation.redis.enabled) {
      properties.put("otel.instrumentation.jedis.enabled", "true");
      properties.put("otel.instrumentation.lettuce.enabled", "true");
    }
    if (config.instrumentation.springScheduling.enabled) {
      properties.put("otel.instrumentation.spring-scheduling.enabled", "true");
      // this is needed for the job.system attribute in order to map those spans to requests
      properties.put("otel.instrumentation.spring-scheduling.experimental-span-attributes", "true");
    }
    if (config.preview.captureLogbackCodeAttributes) {
      properties.put(
          "otel.instrumentation.logback-appender.experimental.capture-code-attributes", "true");
    }
    if (config.preview.captureLogbackMarker) {
      properties.put(
          "otel.instrumentation.logback-appender.experimental.capture-marker-attribute", "true");
    }
    if (config.preview.captureLog4jMarker) {
      properties.put(
          "otel.instrumentation.log4j-appender.experimental.capture-marker-attribute", "true");
    }
    if (config.preview.instrumentation.akka.enabled) {
      properties.put("otel.instrumentation.akka-actor.enabled", "true");
      properties.put("otel.instrumentation.akka-http.enabled", "true");
      properties.put("otel.instrumentation.scala-fork-join.enabled", "true");
    }
    if (config.preview.instrumentation.play.enabled) {
      properties.put("otel.instrumentation.play-mvc.enabled", "true");
      properties.put("otel.instrumentation.play-ws.enabled", "true");
      properties.put("otel.instrumentation.scala-fork-join.enabled", "true");
    }
    if (config.preview.instrumentation.apacheCamel.enabled) {
      properties.put("otel.instrumentation.apache-camel.enabled", "true");
    }
    if (config.preview.instrumentation.grizzly.enabled) {
      // note: grizzly instrumentation is off by default upstream
      properties.put("otel.instrumentation.grizzly.enabled", "true");
    }
    if (config.preview.instrumentation.springIntegration.enabled) {
      properties.put("otel.instrumentation.spring-integration.enabled", "true");
    }
    if (config.preview.instrumentation.vertx.enabled) {
      properties.put("otel.instrumentation.vertx.enabled", "true");
      // the hibernate-reactive instrumentation is needed
      // in order to propagate context to vertx-sql-client
      properties.put("otel.instrumentation.hibernate-reactive.enabled", "true");
    }
    if (config.preview.instrumentation.ktor.enabled) {
      properties.put("otel.instrumentation.ktor.enabled", "true");
    }
    if (config.preview.instrumentation.jaxrsAnnotations.enabled) {
      properties.put("otel.instrumentation.jaxrs-1.0.enabled", "true");
      properties.put("otel.instrumentation.jaxrs-annotations.enabled", "true");
    }
    if (config.preview.instrumentation.r2dbc.enabled) {
      properties.put("otel.instrumentation.r2dbc.enabled", "true");
    }
  }

  private static void setHttpHeaderConfiguration(
      Map<String, String> properties, String propertyName, List<String> headers) {
    if (!headers.isEmpty()) {
      properties.put(propertyName, join(headers, ','));
    }
  }

  private static <T> String join(List<T> values, char separator) {
    StringBuilder sb = new StringBuilder();
    for (Object val : values) {
      if (sb.length() > 0) {
        sb.append(separator);
      }
      sb.append(val);
    }
    return sb.toString();
  }
}
