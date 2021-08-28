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
    if (!config.preview.openTelemetryApiSupport) {
      properties.put("otel.instrumentation.opentelemetry-api.enabled", "false");
      properties.put("otel.instrumentation.opentelemetry-api-metrics.enabled", "false");
      // TODO (trask) this is old name, remove after updating to version that has
      //  this: https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3611
      properties.put("otel.instrumentation.opentelemetry-metrics-api.enabled", "false");
      properties.put("otel.instrumentation.opentelemetry-annotations.enabled", "false");
    }
    if (!config.preview.instrumentation.azureSdk.enabled) {
      properties.put("otel.instrumentation.azure-core.enabled", "false");
    }
    if (config.preview.instrumentation.grizzly.enabled) {
      // grizzly instrumentation is off by default
      // TODO (trask) investigate if grizzly instrumentation can be enabled upstream by default now
      properties.put("otel.instrumentation.grizzly.enabled", "true");
    }
    if (!config.preview.instrumentation.springIntegration.enabled) {
      properties.put("otel.instrumentation.spring-integration.enabled", "false");
    }
    properties.put("otel.propagators", DelegatingPropagatorProvider.NAME);
    // AI exporter is configured manually
    properties.put("otel.traces.exporter", "none");
    // this should be none, but see comment in TemporaryNoopMeterConfigurer
    properties.put("otel.metrics.exporter", "noop");

    return new ConfigBuilder().readProperties(properties).build();
  }

  private ConfigOverride() {}
}
