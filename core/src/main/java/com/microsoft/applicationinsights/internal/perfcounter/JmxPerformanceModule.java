package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.annotation.PerformanceModule;

/**
 * Created by gupele on 3/15/2015.
 */
@PerformanceModule(value="jmx")
public class JmxPerformanceModule implements TelemetryModule {
    @Override
    public void initialize(TelemetryConfiguration configuration) {
    }
}
