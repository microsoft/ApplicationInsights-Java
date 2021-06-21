package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesBuilder;
import io.opentelemetry.javaagent.extension.ignore.IgnoredTypesConfigurer;

public class AiIgnoreMatcherProvider implements IgnoredTypesConfigurer {

    @Override
    public void configure(Config config, IgnoredTypesBuilder builder) {
        builder.ignoreClass("com.microsoft.applicationinsights.agent.");
    }
}
