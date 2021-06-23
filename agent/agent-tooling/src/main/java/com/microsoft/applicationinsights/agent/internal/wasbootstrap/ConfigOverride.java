package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.propagator.DelegatingPropagatorProvider;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.instrumentation.api.config.ConfigBuilder;

import static java.util.concurrent.TimeUnit.SECONDS;

class ConfigOverride {

    static Config getConfig(Configuration config) {
        Map<String, String> properties = new HashMap<>();
        properties.put("otel.experimental.log.capture.threshold", config.instrumentation.logging.level);
        properties.put("otel.micrometer.step.millis", Long.toString(SECONDS.toMillis(config.preview.metricIntervalSeconds)));
        if (!config.instrumentation.micrometer.enabled) {
            properties.put("otel.instrumentation.micrometer.enabled", "false");
            properties.put("otel.instrumentation.actuator-metrics.enabled", "false");
        }
        if (!config.instrumentation.jdbc.enabled) {
            properties.put("otel.instrumentation.jdbc.enabled", "false");
        }
        if (!config.instrumentation.redis.enabled) {
            properties.put("otel.instrumentation.jedis.enabled", "false");
            properties.put("otel.instrumentation.lettuce.enabled", "false");
        }
        if (!config.instrumentation.kafka.enabled) {
            properties.put("otel.instrumentation.kafka.enabled", "false");
        }
        if (!config.instrumentation.jms.enabled) {
            properties.put("otel.instrumentation.jms.enabled", "false");
        }
        if (!config.instrumentation.mongo.enabled) {
            properties.put("otel.instrumentation.mongo.enabled", "false");
        }
        if (!config.instrumentation.cassandra.enabled) {
            properties.put("otel.instrumentation.cassandra.enabled", "false");
        }
        if (!config.instrumentation.springScheduling.enabled) {
            properties.put("otel.instrumentation.spring-scheduling.enabled", "false");
        }
        if (!config.preview.openTelemetryApiSupport) {
            properties.put("otel.instrumentation.opentelemetry-api.enabled", "false");
            properties.put("otel.instrumentation.opentelemetry-annotations.enabled", "false");
        }
        if (!config.preview.instrumentation.azureSdk.enabled) {
            properties.put("otel.instrumentation.azure-core.enabled", "false");
        }
        if (!config.preview.instrumentation.javaHttpClient.enabled) {
            properties.put("otel.instrumentation.java-http-client.enabled", "false");
        }
        if (!config.preview.instrumentation.rabbitmq.enabled) {
            properties.put("otel.instrumentation.rabbitmq.enabled", "false");
        }
        if (!config.preview.instrumentation.jaxws.enabled) {
            properties.put("otel.instrumentation.jaxws.enabled", "false");
            properties.put("otel.instrumentation.axis2.enabled", "false");
            properties.put("otel.instrumentation.cxf.enabled", "false");
            properties.put("otel.instrumentation.metro.enabled", "false");
        }
        properties.put("otel.propagators", DelegatingPropagatorProvider.NAME);
        // AI exporter is configured manually
        properties.put("otel.traces.exporter", "none");
        properties.put("otel.metrics.exporter", "none");

        return new ConfigBuilder().readProperties(properties).build();
    }

    private ConfigOverride() {}
}
