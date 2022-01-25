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

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.DelegatingPropagatorProvider;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.instrumentation.api.config.ConfigBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class ConfigOverride {

  static Config getConfig(Configuration config) {
    Map<String, String> properties = new HashMap<>();
    properties.put("otel.experimental.log.capture.threshold", config.instrumentation.logging.level);
    properties.put(
        "otel.micrometer.step.millis",
        Long.toString(SECONDS.toMillis(config.preview.metricIntervalSeconds)));
    if (!config.instrumentation.micrometer.enabled) {
      properties.put("otel.instrumentation.micrometer.enabled", "false");
      properties.put("otel.instrumentation.actuator-metrics.enabled", "false");
    }
    if (!config.instrumentation.azureSdk.enabled) {
      properties.put("otel.instrumentation.azure-core.enabled", "false");
    }
    if (!config.instrumentation.cassandra.enabled) {
      properties.put("otel.instrumentation.cassandra.enabled", "false");
    }
    if (!config.instrumentation.jdbc.enabled) {
      properties.put("otel.instrumentation.jdbc.enabled", "false");
    }
    if (!config.instrumentation.jms.enabled) {
      properties.put("otel.instrumentation.jms.enabled", "false");
    }
    if (!config.instrumentation.kafka.enabled) {
      properties.put("otel.instrumentation.kafka.enabled", "false");
    }
    if (!config.instrumentation.mongo.enabled) {
      properties.put("otel.instrumentation.mongo.enabled", "false");
    }
    if (!config.instrumentation.rabbitmq.enabled) {
      // TODO (trask) add test for RabbitMQ and a test for disabled RabbitMQ
      properties.put("otel.instrumentation.rabbitmq.enabled", "false");
    }
    if (!config.instrumentation.redis.enabled) {
      properties.put("otel.instrumentation.jedis.enabled", "false");
      properties.put("otel.instrumentation.lettuce.enabled", "false");
    }
    if (!config.instrumentation.springScheduling.enabled) {
      properties.put("otel.instrumentation.spring-scheduling.enabled", "false");
    }
    if (!config.preview.instrumentation.akka.enabled) {
      // akka instrumentation is ON by default in OTEL
      properties.put("otel.instrumentation.akka-actor.enabled", "false");
      properties.put("otel.instrumentation.akka-http.enabled", "false");
    }
    if (!config.preview.instrumentation.play.enabled) {
      // play instrumentation is ON by default in OTEL
      properties.put("otel.instrumentation.play.enabled", "false");
    }
    if (!config.preview.instrumentation.apacheCamel.enabled) {
      // apache-camel instrumentation is ON by default in OTEL
      properties.put("otel.instrumentation.apache-camel.enabled", "false");
    }
    if (config.preview.instrumentation.grizzly.enabled) {
      // grizzly instrumentation is off by default
      // TODO (trask) investigate if grizzly instrumentation can be enabled upstream by default now
      properties.put("otel.instrumentation.grizzly.enabled", "true");
    }
    if (!config.preview.instrumentation.quartz.enabled) {
      // quartz instrumentation is ON by default in OTEL
      properties.put("otel.instrumentation.quartz.enabled", "false");
    }
    if (!config.preview.instrumentation.springIntegration.enabled) {
      // springIntegration instrumentation is ON by default in OTEL
      properties.put("otel.instrumentation.spring-integration.enabled", "false");
    }
    if (!config.preview.instrumentation.vertx.enabled) {
      // vertx instrumentation is ON by default in OTEL
      properties.put("otel.instrumentation.vertx.enabled", "false");
    }
    if (!config.preview.captureControllerSpans) {
      properties.put("otel.instrumentation.common.experimental.suppress-controller-spans", "true");
    }
    properties.put("otel.instrumentation.common.experimental.suppress-view-spans", "true");
    properties.put(
        "otel.instrumentation.common.experimental.suppress-messaging-receive-spans", "true");
    // this is needed to capture kafka.record.queue_time_ms
    properties.put("otel.instrumentation.kafka.experimental-span-attributes", "true");

    setHttpHeaderConfiguration(
        properties,
        "otel.instrumentation.http.capture-headers.server.request",
        config.preview.captureHttpServerHeaders.requestHeaders);
    setHttpHeaderConfiguration(
        properties,
        "otel.instrumentation.http.capture-headers.server.response",
        config.preview.captureHttpServerHeaders.responseHeaders);
    setHttpHeaderConfiguration(
        properties,
        "otel.instrumentation.http.capture-headers.client.request",
        config.preview.captureHttpClientHeaders.requestHeaders);
    setHttpHeaderConfiguration(
        properties,
        "otel.instrumentation.http.capture-headers.client.response",
        config.preview.captureHttpClientHeaders.responseHeaders);

    properties.put("otel.propagators", DelegatingPropagatorProvider.NAME);

    String tracesExporter = getProperty("otel.traces.exporter");
    if (tracesExporter == null) {
      // currently Application Insights Exporter has to be configured manually because it relies on
      // using a BatchSpanProcessor with queue size 1 due to live metrics (this will change in the
      // future)
      properties.put("otel.traces.exporter", "none");

      // TODO (trask) this can go away once new indexer is rolled out to gov clouds
      List<String> httpClientResponseHeaders = new ArrayList<>();
      httpClientResponseHeaders.add("request-context");
      httpClientResponseHeaders.addAll(config.preview.captureHttpClientHeaders.responseHeaders);
      setHttpHeaderConfiguration(
          properties,
          "otel.instrumentation.http.capture-headers.client.response",
          httpClientResponseHeaders);
    } else {
      properties.put("otel.traces.exporter", tracesExporter);
    }

    String metricsExporter = getProperty("otel.metrics.exporter");
    if (metricsExporter == null) {
      // currently Application Insights exports metrics directly, not through OpenTelemetry
      // exporter (this will change in the future)
      properties.put("otel.metrics.exporter", "none");
    } else {
      properties.put("otel.metrics.exporter", metricsExporter);
    }

    String logsExporter = getProperty("otel.logs.exporter");
    if (logsExporter == null) {
      // currently Application Insights exports logs directly, not through OpenTelemetry
      // exporter (this will change in the future)
      properties.put("otel.logs.exporter", "none");
    } else {
      properties.put("otel.logs.exporter", logsExporter);
    }

    if (config.role.name != null) {
      // in case using another exporter
      properties.put("otel.service.name", config.role.name);
    }

    return new ConfigBuilder().readProperties(properties).build();
  }

  private static void setHttpHeaderConfiguration(
      Map<String, String> properties, String propertyName, List<String> headers) {
    if (!headers.isEmpty()) {
      properties.put(propertyName, join(headers, ','));
    }
  }

  private static String getProperty(String propertyName) {
    String value = System.getProperty(propertyName);
    if (value != null) {
      return value;
    }
    String envVarName = propertyName.replace('.', '_').toUpperCase(Locale.ROOT);
    return System.getenv(envVarName);
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

  private ConfigOverride() {}
}
