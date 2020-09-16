package io.opentelemetry.auto.config;

import java.util.Properties;

public class ConfigOverride {

    private static volatile Properties properties;

    public static void set(Properties properties) {
        ConfigOverride.properties = properties;
    }

    public static Properties get() {
        return properties;
    }
}
