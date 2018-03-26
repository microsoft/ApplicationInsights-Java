package com.microsoft.applicationinsights.smoketest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import com.microsoft.applicationinsights.internal.schemav2.PerformanceCounterData;

import org.junit.*;

import static org.junit.Assert.*;

public class PerfCountersDataTest extends AiSmokeTest {
    @Test
    public void testPerformanceCounterData() throws Exception {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> x = executor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpHelper.get(getBaseUrl() + "/index.jsp");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 5, TimeUnit.SECONDS); // this just calls the uri after 5 seconds.

        System.out.println("Waiting for performance data...");
        long start = System.currentTimeMillis();
        List<Envelope> performanceItems =
                // this is the new method
                mockedIngestion.waitForItems(new Predicate<Envelope>() { // this Predicate object lets you check for the
                    // item you are looking for.
                    @Override
                    public boolean apply(@Nullable Envelope input) {
                        if (input == null) {
                            return false;
                        }
                        // here, we are just checking if the data type == "PerformanceCounterData", but you could check for more conditions.
                        boolean rval = "PerformanceCounterData".equals(input.getData().getBaseType());
                        return rval;
                    }
                }, 8, // waits for 8 items
                        150, TimeUnit.SECONDS); // times out after 150 seconds (it will throw a TimeoutException)

        assertEquals(8, performanceItems.size());
        for (Envelope item : performanceItems) {
            assertEquals("PerformanceCounterData", item.getData().getBaseType());
        }
        System.out.println("PerformanceCounterData are good: " + (System.currentTimeMillis() - start));

        List<Envelope> metricItems = mockedIngestion.waitForItems(new Predicate<Envelope>() {
            @Override
            public boolean apply(@Nullable Envelope input) {
                if (input == null) {
                    return false;
                }
                boolean rval = "MetricData".equals(input.getData().getBaseType());
                return rval;
            }
        }, 4, 10, TimeUnit.SECONDS);
        assertEquals(4, metricItems.size());
        for (Envelope item : metricItems) {
            assertEquals("MetricData", item.getData().getBaseType());
        }
        System.out.println("MetricData are good: " + (System.currentTimeMillis() - start));

        System.out.println("Waiting for requests...");
        start = System.currentTimeMillis();
        Envelope requestItem = mockedIngestion.waitForItem(new Predicate<Envelope>() {
            @Override
            public boolean apply(@Nullable Envelope input) {
                if (input == null)
                    return false;
                return "RequestData".equals(input.getData().getBaseType());
            }
        }, 10, TimeUnit.SECONDS); // this one just returns a single envelope.
        // at this point, the item has already arrived and this just returns immediately
        assertNotNull(requestItem);
        System.out.println("Requests are good: " + (System.currentTimeMillis() - start));

        DataPoint dp1 = getMetricDataDetails(2);
        assertEquals("Heap Memory Used (MB)", dp1.getName());
        assertEquals(DataPointType.Measurement, dp1.getKind());
        assertTrue(dp1.getValue() > 0);

        DataPoint dp2 = getMetricDataDetails(3);
        assertEquals("Suspected Deadlocked Threads", dp2.getName());
        assertEquals(DataPointType.Measurement, dp2.getKind());
        assertTrue(dp2.getValue() == 0);

        PerformanceCounterData pcfd1 = getTelemetryDataForType(3, "PerformanceCounterData");
        assertEquals("Processor", pcfd1.getCategoryName());
        assertEquals("% Processor Time", pcfd1.getCounterName());
        assertEquals("_Total", pcfd1.getInstanceName());
        assertTrue(pcfd1.getValue() != 0);

        PerformanceCounterData pcd2 = getTelemetryDataForType(4, "PerformanceCounterData");
        assertEquals("Memory", pcd2.getCategoryName());
        assertEquals("Available Bytes", pcd2.getCounterName());
        assertTrue(pcd2.getValue() != 0);

        PerformanceCounterData pcd3 = getTelemetryDataForType(5, "PerformanceCounterData");
        assertEquals("Process", pcd3.getCategoryName());
        assertEquals("Private Bytes", pcd3.getCounterName());
        assertTrue(pcd3.getValue() != 0);

        PerformanceCounterData pcd4 = getTelemetryDataForType(6, "PerformanceCounterData");
        assertEquals("Process", pcd4.getCategoryName());
        assertEquals("IO Data Bytes/sec", pcd4.getCounterName());
        assertTrue(pcd4.getValue() != 0);

        PerformanceCounterData pcd5 = getTelemetryDataForType(7, "PerformanceCounterData");
        assertEquals("Process", pcd5.getCategoryName());
        assertEquals("% Processor Time", pcd5.getCounterName());
        assertTrue(pcd5.getValue() != 0);
    }

    private DataPoint getMetricDataDetails(int index) {
        MetricData md = getTelemetryDataForType(index, "MetricData");
        List<DataPoint> metrics = md.getMetrics();
        assertEquals(1, metrics.size());
        DataPoint dp = metrics.get(0);
        return dp;
    }

}