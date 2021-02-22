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
        properties.put("otel.experimental.log.capture.threshold", getLoggingFrameworksThreshold(config, "INFO"));
        int micrometerIntervalSeconds = getMicrometerIntervalSeconds(config, 60);
        properties.put("otel.micrometer.step.millis", Long.toString(SECONDS.toMillis(micrometerIntervalSeconds)));
        // TODO need some kind of test for these configuration properties
        if (!isInstrumentationEnabled(config, "micrometer")) {
            properties.put("otel.instrumentation.micrometer.enabled", "false");
            properties.put("otel.instrumentation.actuator-metrics.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "jdbc")) {
            properties.put("otel.instrumentation.jdbc.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "redis")) {
            properties.put("otel.instrumentation.jedis.enabled", "false");
            properties.put("otel.instrumentation.lettuce.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "kafka")) {
            properties.put("otel.instrumentation.kafka.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "jms")) {
            properties.put("otel.instrumentation.jms.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "mongo")) {
            properties.put("otel.instrumentation.mongo.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "cassandra")) {
            properties.put("otel.instrumentation.cassandra.enabled", "false");
        }
        if (!isInstrumentationEnabled(config, "spring-scheduling")) {
            properties.put("otel.instrumentation.spring-scheduling.enabled", "false");
        }
        if (!config.preview.openTelemetryApiSupport) {
            properties.put("otel.instrumentation.opentelemetry-api.enabled", "false");
        }
        properties.put("otel.propagators", DelegatingPropagatorProvider.NAME);
        // AI exporter is configured manually
        properties.put("otel.traces.exporter", "none");
        properties.put("otel.metrics.exporter", "none");

        return new ConfigBuilder().readProperties(properties).build();
    }

    private static String getLoggingFrameworksThreshold(Configuration config, String defaultValue) {
        Map<String, Object> logging = config.instrumentation.get("logging");
        if (logging == null) {
            return defaultValue;
        }
        Object levelObj = logging.get("level");
        if (levelObj == null) {
            return defaultValue;
        }
        if (!(levelObj instanceof String)) {
            throw new IllegalStateException("logging level must be a string, but found: " + levelObj.getClass());
        }
        String threshold = (String) levelObj;
        if (threshold.isEmpty()) {
            return defaultValue;
        }
        return threshold;
    }

    private static boolean isInstrumentationEnabled(Configuration config, String instrumentationName) {
        Map<String, Object> properties = config.instrumentation.get(instrumentationName);
        if (properties == null) {
            return true;
        }
        Object value = properties.get("enabled");
        if (value == null) {
            return true;
        }
        if (!(value instanceof Boolean)) {
            throw new IllegalStateException(instrumentationName + " enabled must be a boolean, but found: " + value.getClass());
        }
        return (Boolean) value;
    }

    private static int getMicrometerIntervalSeconds(Configuration config, int defaultValue) {
        Map<String, Object> micrometer = config.instrumentation.get("micrometer");
        if (micrometer == null) {
            return defaultValue;
        }
        Object value = micrometer.get("intervalSeconds");
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number)) {
            throw new IllegalStateException("micrometer intervalSeconds must be a number, but found: " + value.getClass());
        }
        return ((Number) value).intValue();
    }
}
