package com.microsoft.applicationinsights.internal.perfcounter;

import java.util.ArrayList;

import com.microsoft.applicationinsights.internal.system.SystemInformation;

import org.junit.Test;

public final class WindowsPerformanceCounterAsMetricTest {
    @Test(expected = Throwable.class)
    public void testNoNativeCodeActivated() throws Throwable {
        if (!SystemInformation.INSTANCE.isWindows()) {
            return;
        }

        ArrayList<WindowsPerformanceCounterData> data = new ArrayList<WindowsPerformanceCounterData>();
        data.add(new WindowsPerformanceCounterData().
                        setCategoryName("Category").
                        setCounterName("Counter").
                        setInstanceName("Instance").
                        setDisplayName("display"));
        WindowsPerformanceCounterAsMetric pc = new WindowsPerformanceCounterAsMetric(data);
    }

    @Test(expected = NullPointerException.class)
    public void testNull() throws Throwable {
        if (!SystemInformation.INSTANCE.isWindows()) {
            return;
        }

        WindowsPerformanceCounterAsMetric pc = new WindowsPerformanceCounterAsMetric(null);
    }
}