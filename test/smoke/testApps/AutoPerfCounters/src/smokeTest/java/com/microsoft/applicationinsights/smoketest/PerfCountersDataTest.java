package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import org.junit.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.Assert.*;

@UseAgent("fastmetrics")
@SuppressWarnings("deprecation")
public class PerfCountersDataTest extends AiSmokeTest {
    @Test
    @TargetUri(value = "index.jsp", delay = 5000)
    public void testPerformanceCounterData() throws Exception {
        System.out.println("Waiting for performance data...");
        long start = System.currentTimeMillis();

        // need to accommodate for START_COLLECTING_DELAY_IN_MILLIS = 60 seconds
        int timeout = 70;

        Envelope availableMem = mockedIngestion.waitForItem(getPerfMetricPredicate("\\Memory\\Available Bytes"), timeout, TimeUnit.SECONDS);
        Envelope totalCpu = mockedIngestion.waitForItem(getPerfMetricPredicate("\\Processor(_Total)\\% Processor Time"), timeout, TimeUnit.SECONDS);

        Envelope processIo = mockedIngestion.waitForItem(getPerfMetricPredicate("\\Process(??APP_WIN32_PROC??)\\IO Data Bytes/sec"), timeout, TimeUnit.SECONDS);
        Envelope processMemUsed = mockedIngestion.waitForItem(getPerfMetricPredicate("\\Process(??APP_WIN32_PROC??)\\Private Bytes"), timeout, TimeUnit.SECONDS);
        Envelope processCpu = mockedIngestion.waitForItem(getPerfMetricPredicate("\\Process(??APP_WIN32_PROC??)\\% Processor Time"), timeout, TimeUnit.SECONDS);
        System.out.println("PerformanceCounterData are good: " + (System.currentTimeMillis() - start));

        MetricData metricMem = getBaseData(availableMem);
        assertPerfMetric(metricMem);
        assertEquals("\\Memory\\Available Bytes", metricMem.getMetrics().get(0).getName());

        MetricData pdCpu = getBaseData(totalCpu);
        assertPerfMetric(pdCpu);
        assertEquals("\\Processor(_Total)\\% Processor Time", pdCpu.getMetrics().get(0).getName());

        assertPerfMetric(getBaseData(processIo));
        assertPerfMetric(getBaseData(processMemUsed));
        assertPerfMetric(getBaseData(processCpu));

        start = System.currentTimeMillis();
        System.out.println("Waiting for metric data...");
        Envelope deadlocks = mockedIngestion.waitForItem(getPerfMetricPredicate("Suspected Deadlocked Threads"), timeout, TimeUnit.SECONDS);
        Envelope heapUsed = mockedIngestion.waitForItem(getPerfMetricPredicate("Heap Memory Used (MB)"), timeout, TimeUnit.SECONDS);
        Envelope gcTotalCount = mockedIngestion.waitForItem(getPerfMetricPredicate("GC Total Count"), timeout, TimeUnit.SECONDS);
        Envelope gcTotalTime = mockedIngestion.waitForItem(getPerfMetricPredicate("GC Total Time"), timeout, TimeUnit.SECONDS);
        System.out.println("MetricData are good: " + (System.currentTimeMillis() - start));

        MetricData mdDeadlocks = getBaseData(deadlocks);
        assertPerfMetric(mdDeadlocks);
        assertEquals(0.0, mdDeadlocks.getMetrics().get(0).getValue(), Math.ulp(0.0));

        MetricData mdHeapUsed = getBaseData(heapUsed);
        assertPerfMetric(mdHeapUsed);
        assertTrue(mdHeapUsed.getMetrics().get(0).getValue() > 0.0);

        MetricData mdGcTotalCount = getBaseData(gcTotalCount);
        assertPerfMetric(mdGcTotalCount);

        MetricData mdGcTotalTime = getBaseData(gcTotalTime);
        assertPerfMetric(mdGcTotalTime);
    }

    private void assertPerfMetric(MetricData perfMetric) {
        List<DataPoint> metrics = perfMetric.getMetrics();
        assertEquals(1, metrics.size());
        DataPoint dp = metrics.get(0);
        assertEquals(DataPointType.Measurement, dp.getKind());
    }

    private static Predicate<Envelope> getPerfMetricPredicate(String name) {
        Objects.requireNonNull(name, "name");
        return new Predicate<Envelope>() {
            @Override
            public boolean test(Envelope input) {
                if(!input.getData().getBaseType().equals("MetricData")) {
                    return false;
                }
                MetricData md = getBaseData(input);
                return name.equals(md.getMetrics().get(0).getName());
            }
        };
    }

}