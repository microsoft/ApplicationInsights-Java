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
import java.util.HashMap;
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
    if (!config.preview.instrumentation.apacheCamel.enabled) {
      properties.put("otel.instrumentation.apache-camel.enabled", "false");
    }
    if (config.preview.instrumentation.grizzly.enabled) {
      // grizzly instrumentation is off by default
      // TODO (trask) investigate if grizzly instrumentation can be enabled upstream by default now
      properties.put("otel.instrumentation.grizzly.enabled", "true");
    }
    if (!config.preview.instrumentation.quartz.enabled) {
      properties.put("otel.instrumentation.quartz.enabled", "false");
    }
    if (!config.preview.instrumentation.springIntegration.enabled) {
      properties.put("otel.instrumentation.spring-integration.enabled", "false");
    }
    if (!config.preview.captureControllerSpans) {
      properties.put("otel.instrumentation.common.experimental.suppress-controller-spans", "true");
    }
    properties.put(
        "otel.instrumentation.common.experimental.suppress-messaging-receive-spans", "true");
    // this is needed to capture kafka.record.queue_time_ms
    properties.put("otel.instrumentation.kafka.experimental-span-attributes", "true");

    properties.put("otel.propagators", DelegatingPropagatorProvider.NAME);

    String tracesExporter = System.getProperty("otel.traces.exporter");
    if (tracesExporter == null) {
      tracesExporter = System.getenv("OTEL_TRACES_EXPORTER");
    }
    if (tracesExporter == null) {
      // currently Application Insights Exporter has to be configured manually because it relies on
      // using a BatchSpanProcessor with queue size 1 due to live metrics (this will change in the
      // future)
      properties.put("otel.traces.exporter", "none");
    } else {
      properties.put("otel.traces.exporter", tracesExporter);
    }

    String metricsExporter = System.getProperty("otel.metrics.exporter");
    if (metricsExporter == null) {
      metricsExporter = System.getenv("OTEL_METRICS_EXPORTER");
    }
    if (metricsExporter == null) {
      // currently Application Insights exports metrics directly, not through OpenTelemetry
      // exporter (this will change in the future)
      properties.put("otel.metrics.exporter", "none");
    } else {
      properties.put("otel.metrics.exporter", metricsExporter);
    }

    return new ConfigBuilder().readProperties(properties).build();
  }

  private ConfigOverride() {}
}
