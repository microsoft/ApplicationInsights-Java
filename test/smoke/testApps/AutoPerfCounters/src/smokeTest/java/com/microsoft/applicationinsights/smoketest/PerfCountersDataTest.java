package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.microsoft.applicationinsights.internal.schemav2.Base;
import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import com.microsoft.applicationinsights.internal.schemav2.PerformanceCounterData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class PerfCountersDataTest extends AiSmokeTest {
  private static Predicate<Envelope> getPerfCounterPredicate(String category, String counter) {
    return getPerfCounterPredicate(category, counter, null);
  }

  private static Predicate<Envelope> getPerfCounterPredicate(
      String category, String counter, String instance) {
    Preconditions.checkNotNull(category, "category");
    Preconditions.checkNotNull(counter, "counter");
    return new Predicate<Envelope>() {
      @Override
      public boolean apply(@Nullable Envelope input) {
        Base data = input.getData();
        if (!data.getBaseType().equals("PerformanceCounterData")) {
          return false;
        }
        PerformanceCounterData pcd = getBaseData(input);
        return category.equals(pcd.getCategoryName())
            && counter.equals(pcd.getCounterName())
            && (instance == null || instance.equals(pcd.getInstanceName()));
      }
    };
  }

  private static Predicate<Envelope> getPerfMetricPredicate(String name) {
    Preconditions.checkNotNull(name, "name");
    return new Predicate<Envelope>() {
      @Override
      public boolean apply(@Nullable Envelope input) {
        if (!input.getData().getBaseType().equals("MetricData")) {
          return false;
        }
        MetricData md = getBaseData(input);
        return name.equals(md.getMetrics().get(0).getName());
      }
    };
  }

  @Test
  @TargetUri(value = "index.jsp", delay = 5000)
  public void testPerformanceCounterData() throws Exception {
    System.out.println("Waiting for performance data...");
    long start = System.currentTimeMillis();

    // we should get these envelopes:
    //      MetricData, metrics.name="Suspected Deadlocked Threads"
    //      PerformanceCounterData, categoryName=Memory, counterName=Available Bytes
    //      MetricData, metrics.name="Heap Memory Used (MB)"
    //      PerformanceCounterData, category=Process, counter="IO Data Bytes/sec"
    //      PerformanceCounterData, category=Processor, counter="% Processor Time",
    // instance="_Total"
    //      PerformanceCounterData, category=Process, counter=Private Bytes
    //      PerformanceCounterData, Process, "% Processor Time"

    Envelope availableMem =
        mockedIngestion.waitForItem(
            getPerfCounterPredicate("Memory", "Available Bytes"), 150, TimeUnit.SECONDS);
    Envelope totalCpu =
        mockedIngestion.waitForItem(
            getPerfCounterPredicate("Processor", "% Processor Time", "_Total"),
            150,
            TimeUnit.SECONDS);

    Envelope processIo =
        mockedIngestion.waitForItem(
            getPerfCounterPredicate("Process", "IO Data Bytes/sec"), 150, TimeUnit.SECONDS);
    Envelope processMemUsed =
        mockedIngestion.waitForItem(
            getPerfCounterPredicate("Process", "Private Bytes"), 150, TimeUnit.SECONDS);
    Envelope processCpu =
        mockedIngestion.waitForItem(
            getPerfCounterPredicate("Process", "% Processor Time"), 150, TimeUnit.SECONDS);
    System.out.println("PerformanceCounterData are good: " + (System.currentTimeMillis() - start));

    PerformanceCounterData pdMem = getBaseData(availableMem);
    assertPerfCounter(pdMem);
    assertNull(pdMem.getInstanceName());

    PerformanceCounterData pdCpu = getBaseData(totalCpu);
    assertPerfCounter(pdCpu);
    assertEquals("_Total", pdCpu.getInstanceName());

    assertPerfCounter(getBaseData(processIo));
    assertPerfCounter(getBaseData(processMemUsed));
    assertPerfCounter(getBaseData(processCpu));
    assertSameInstanceName(processIo, processMemUsed, processCpu);

    start = System.currentTimeMillis();
    System.out.println("Waiting for metric data...");
    Envelope deadlocks =
        mockedIngestion.waitForItem(
            getPerfMetricPredicate("Suspected Deadlocked Threads"), 150, TimeUnit.SECONDS);
    Envelope heapUsed =
        mockedIngestion.waitForItem(
            getPerfMetricPredicate("Heap Memory Used (MB)"), 150, TimeUnit.SECONDS);
    System.out.println("MetricData are good: " + (System.currentTimeMillis() - start));

    MetricData mdDeadlocks = getBaseData(deadlocks);
    assertPerfMetric(mdDeadlocks);
    assertEquals(0.0, mdDeadlocks.getMetrics().get(0).getValue(), Math.ulp(0.0));

    MetricData mdHeapUsed = getBaseData(heapUsed);
    assertPerfMetric(mdHeapUsed);
    assertTrue(mdHeapUsed.getMetrics().get(0).getValue() > 0.0);
  }

  private void assertSameInstanceName(Envelope... envelopes) {
    Preconditions.checkArgument(envelopes.length > 0);
    PerformanceCounterData firstOne = getBaseData(envelopes[0]);
    String instanceName = firstOne.getInstanceName();
    assertNotNull(instanceName);
    if (envelopes.length == 1) {
      return;
    }
    for (int i = 1; i < envelopes.length; i++) {
      PerformanceCounterData pcd = getBaseData(envelopes[i]);
      assertEquals(instanceName, pcd.getInstanceName());
    }
  }

  private void assertPerfCounter(PerformanceCounterData perfCounter) {
    assertTrue(perfCounter.getValue() > 0.0);
  }

  private void assertPerfMetric(MetricData perfMetric) {
    assertEquals("true", perfMetric.getProperties().get("CustomPerfCounter"));
    List<DataPoint> metrics = perfMetric.getMetrics();
    assertEquals(1, metrics.size());
    DataPoint dp = metrics.get(0);
    assertEquals(DataPointType.Measurement, dp.getKind());
  }
}
