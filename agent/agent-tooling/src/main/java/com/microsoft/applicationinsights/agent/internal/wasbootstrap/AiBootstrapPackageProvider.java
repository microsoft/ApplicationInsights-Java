package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import java.util.Arrays;
import java.util.List;

import io.opentelemetry.javaagent.spi.BootstrapPackagesProvider;

public class AiBootstrapPackageProvider implements BootstrapPackagesProvider {

    @Override
    public List<String> getPackagePrefixes() {
        return Arrays.asList("com.microsoft.applicationinsights.agent.bootstrap");
    }
}
