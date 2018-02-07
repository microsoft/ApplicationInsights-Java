package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import java.util.List;
import org.junit.Test;

public class SimpleTrackMetricDataTest extends AiSmokeTest {

    @Test
    @TargetUri("/trackMetric?leftOperand=1&rightOperand=2&operator=plus")
    public void trackMetric() throws Exception {
        assertEquals(2, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("MetricData"));
        int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 3;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
                expectedItems, totalItems);
                
        // TODO get Metric data envelope and verify value
        MetricData d = getTelemetryDataForType(0, "MetricData");
        List<DataPoint> metrics = d.getMetrics();
		assertEquals(1, metrics.size());
        DataPoint dp = metrics.get(0);
        
		final double expectedValue = 111222333.0;
		final double epsilon = Math.ulp(expectedValue);
		assertEquals(DataPointType.Measurement, dp.getKind());
		assertEquals(expectedValue, dp.getValue(), epsilon);
		assertEquals("TimeToRespond", dp.getName());
		assertEquals(Integer.valueOf(1),  dp.getCount());

		assertNull(dp.getMin());
		assertNull(dp.getMax());
		assertNull(dp.getStdDev());
    }
}
