package com.microsoft.applicationinsights.internal.perfcounter;

/**
 * An interface that is used by factories that know to create Windows
 * based on data that is given in a container of {@link com.microsoft.applicationinsights.internal.perfcounter.WindowsPerformanceCounterData}
 *
 * Created by gupele on 3/31/2015.
 */
public interface WindowsPerformanceCountersFactory {
    void setWindowsPCs(Iterable<WindowsPerformanceCounterData> all);
}
