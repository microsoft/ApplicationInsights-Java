package com.microsoft.applicationinsights.internal.perfcounter;

import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public final class ProcessPerformanceCountersModuleTest {
    @Test(expected = Exception.class)
    public void testFactoryIsConfigurationAware() throws Exception {
        new ProcessPerformanceCountersModule(new PerformanceCountersFactory() {
            @Override
            public Collection<PerformanceCounter> getPerformanceCounters() {
                return null;
            }
        });
    }
}