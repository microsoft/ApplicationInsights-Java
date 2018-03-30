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

        System.out.println("Waiting for metric data...");
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

        int heapMemoryUsed = 0;
        int suspected = 0;
        for (int i = metricItems.size(); i > 2; i = i - 1) {
            DataPoint dp1 = getMetricDataDetails(i - 1);
            switch (dp1.getName()) {
            case "Suspected Deadlocked Threads":
                assertEquals(DataPointType.Measurement, dp1.getKind());
                assertTrue(dp1.getValue() == 0);
                suspected++;
                break;
            case "Heap Memory Used (MB)":
                assertEquals(DataPointType.Measurement, dp1.getKind());
                assertTrue(dp1.getValue() > 0);
                heapMemoryUsed++;
                break;
            default:
                break;
            }
        }
        assertEquals(1, heapMemoryUsed);
        assertEquals(1, suspected);

        int processor = 0, processPrivate = 0, processIO = 0, processTime = 0, memory = 0;
        for (int i = performanceItems.size(); i > 3; i = i - 1) {
            PerformanceCounterData perfd1 = getTelemetryDataForType(i - 1, "PerformanceCounterData");
            switch (perfd1.getCategoryName()) {
            case "Processor":
                assertEquals("% Processor Time", perfd1.getCounterName());
                assertEquals("_Total", perfd1.getInstanceName());
                processor++;
                break;
            case "Memory":
                assertEquals("Available Bytes", perfd1.getCounterName());
                assertTrue(perfd1.getValue() != 0);
                memory++;
            case "Process":
                assertTrue(perfd1.getValue() != 0);
                switch (perfd1.getCounterName()) {
                case "Private Bytes":
                    processPrivate++;
                    break;
                case "IO Data Bytes/sec":
                    processIO++;
                    break;
                case "% Processor Time":
                    processTime++;
                    break;
                default:
                    break;
                }
            default:
                break;
            }
        }

        assertEquals(1, processor);
        assertEquals(1, processPrivate);
        assertEquals(1, processIO);
        assertEquals(1, processTime);
        assertEquals(1, memory);
    }

    private DataPoint getMetricDataDetails(int index) {
        MetricData md = getTelemetryDataForType(index, "MetricData");
        List<DataPoint> metrics = md.getMetrics();
        assertEquals(1, metrics.size());
        DataPoint dp = metrics.get(0);
        return dp;
    }

}