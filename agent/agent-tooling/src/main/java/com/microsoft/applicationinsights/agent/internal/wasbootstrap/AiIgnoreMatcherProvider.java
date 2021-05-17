package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import io.opentelemetry.javaagent.spi.IgnoreMatcherProvider;
import net.bytebuddy.description.type.TypeDescription;

public class AiIgnoreMatcherProvider implements IgnoreMatcherProvider {

    @Override
    public Result type(TypeDescription target) {
        if (target.getActualName().startsWith("com.microsoft.applicationinsights.agent.")) {
            return Result.IGNORE;
        }
        return Result.DEFAULT;
    }

    @Override
    public Result classloader(ClassLoader classLoader) {
        return Result.DEFAULT;
    }
}
