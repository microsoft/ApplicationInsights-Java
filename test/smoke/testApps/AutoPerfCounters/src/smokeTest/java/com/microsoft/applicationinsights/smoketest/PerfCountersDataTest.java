package com.microsoft.applicationinsights.smoketest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.microsoft.applicationinsights.internal.schemav2.Base;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import com.microsoft.applicationinsights.internal.schemav2.PerformanceCounterData;

import org.junit.*;

import static org.junit.Assert.*;

public class PerfCountersDataTest extends AiSmokeTest {

    private static Predicate<Envelope> getPerfCounterPredicate(String category, String counter) {
        return getPerfCounterPredicate(category, counter, null);
    }

    private static Predicate<Envelope> getPerfCounterPredicate(String category, String counter, String instance) {
        Preconditions.checkNotNull(category, "category");
        Preconditions.checkNotNull(counter, "counter");
        return new Predicate<Envelope>() {
            @Override
            public boolean apply(@Nullable Envelope input) {
                Base data = input.getData();
                if (!data.getBaseType().equals("PerformanceCounterData")) {
                    return false;
                }
                PerformanceCounterData pcd = ((Data<PerformanceCounterData>)data).getBaseData();
                return category.equals(pcd.getCategoryName()) && counter.equals(pcd.getCounterName())
                        && (instance == null || instance.equals(pcd.getInstanceName()));
            }
        };
    }

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

        // we should get these envelopes:
        //      MetricData, metrics.name="Suspected Deadlocked Threads"
        //      PerformanceCounterData, categoryName=Memory, counterName=Available Bytes
        //      MetricData, metrics.name="Heap Memory Used (MB)"
        //      PerformanceCounterData, category=Process, counter="IO Data Bytes/sec"
        //      PerformanceCounterData, category=Processor, counter="% Processor Time", instance="_Total"
        //      PerformanceCounterData, category=Process, counter=Private Bytes
        //      PerformanceCounterData, Process, "% Processor Time"

        List<Envelope> performanceItems = new ArrayList<>();
        performanceItems.add(mockedIngestion.waitForItem(getPerfCounterPredicate("Memory", "Available Bytes"), 150, TimeUnit.SECONDS));
        performanceItems.add(mockedIngestion.waitForItem(getPerfCounterPredicate("Process", "IO Data Bytes/sec"), 150, TimeUnit.SECONDS));
        performanceItems.add(mockedIngestion.waitForItem(getPerfCounterPredicate("Processor", "% Processor Time", "_Total"), 150, TimeUnit.SECONDS));
        performanceItems.add(mockedIngestion.waitForItem(getPerfCounterPredicate("Process", "Private Bytes"), 150, TimeUnit.SECONDS));
        performanceItems.add(mockedIngestion.waitForItem(getPerfCounterPredicate("Process", "% Processor Time"), 150, TimeUnit.SECONDS));

        assertEquals(5, performanceItems.size());
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
        for (Envelope env : performanceItems) {
            PerformanceCounterData perfd1 = ((Data<PerformanceCounterData>)env.getData()).getBaseData();
            assertTrue(perfd1.getValue() > -1);
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
        assertEquals("true", md.getProperties().get("CustomPerfCounter"));
        List<DataPoint> metrics = md.getMetrics();
        assertEquals(1, metrics.size());
        DataPoint dp = metrics.get(0);
        return dp;
    }

}