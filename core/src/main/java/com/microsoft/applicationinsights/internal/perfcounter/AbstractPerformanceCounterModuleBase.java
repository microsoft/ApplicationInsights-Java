package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import java.util.Collection;

/**
 * Created by gupele on 3/12/2015.
 */
public abstract class AbstractPerformanceCounterModuleBase implements TelemetryModule {
    private final PerformanceCountersFactory factory;

    protected AbstractPerformanceCounterModuleBase(PerformanceCountersFactory factory) {
        this.factory = factory;
    }

    @Override
    public void initialize(TelemetryConfiguration configuration) {
        Collection<PerformanceCounter> performanceCounters = factory.getPerformanceCounters();
        for (PerformanceCounter performanceCounter : performanceCounters) {
            try {
                PerformanceCounterContainer.INSTANCE.register(performanceCounter);
            } catch (Throwable e) {
                InternalLogger.INSTANCE.error("Failed to register performance counter: '%s'", e.getMessage());
            }
        }
    }
}
