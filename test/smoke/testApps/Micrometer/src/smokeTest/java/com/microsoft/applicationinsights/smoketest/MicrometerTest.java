package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent("micrometer")
public class MicrometerTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);

        // sometimes receives more than 1 due to timing
        mockedIngestion.waitForMinItems("MetricData", 1);

        List<MetricData> metrics = mockedIngestion.getTelemetryDataByType("MetricData");

        List<DataPoint> points = metrics.get(0).getMetrics();

        assertEquals(1, points.size());

        DataPoint point = points.get(0);

        assertEquals(DataPointType.Measurement, point.getKind());
        assertEquals(1, point.getValue(), 0);
        assertEquals("test_counter", point.getName());
        assertNull("getCount was non-null", point.getCount());
        assertNull("getMin was non-null", point.getMin());
        assertNull("getMax was non-null", point.getMax());
        assertNull("getStdDev was non-null", point.getStdDev());
    }
}
