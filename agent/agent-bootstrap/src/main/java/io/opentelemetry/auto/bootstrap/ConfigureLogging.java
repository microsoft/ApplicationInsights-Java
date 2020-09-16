package io.opentelemetry.auto.bootstrap;

// currently, the existence of this class and method trigger OpenTelemetry auto-instrumentation not to configure its own logging
public class ConfigureLogging {

    public static void configure() {
    }
}
