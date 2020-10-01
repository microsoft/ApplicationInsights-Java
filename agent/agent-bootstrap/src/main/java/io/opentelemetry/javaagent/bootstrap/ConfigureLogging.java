package io.opentelemetry.javaagent.bootstrap;

// currently, the existence of this class and method trigger OpenTelemetry auto-instrumentation not to configure its own logging
public class ConfigureLogging {

    public static void configure() {
    }
}
