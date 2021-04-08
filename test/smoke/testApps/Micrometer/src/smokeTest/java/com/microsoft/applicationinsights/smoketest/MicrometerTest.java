package com.microsoft.applicationinsights.smoketest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent("micrometer")
public class MicrometerTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);

        List<Envelope> metricItems = mockedIngestion.waitForItems(new Predicate<Envelope>() {
            @Override
            public boolean apply(Envelope input) {
                if (!input.getData().getBaseType().equals("MetricData")) {
                    return false;
                }
                MetricData data = (MetricData) ((Data<?>) input.getData()).getBaseData();
                List<DataPoint> points = data.getMetrics();
                DataPoint point = points.get(0);
                return point.getValue() == 1;
            }
        }, 1, 10, TimeUnit.SECONDS);

        MetricData data = (MetricData) ((Data<?>) metricItems.get(0).getData()).getBaseData();
        List<DataPoint> points = data.getMetrics();
        assertEquals(1, points.size());

        DataPoint point = points.get(0);

        assertEquals(DataPointType.Measurement, point.getKind());
        assertEquals(1, point.getValue(), 0); // (this was verified above in Predicate also)
        assertEquals("test_counter", point.getName());
        assertNull("getCount was non-null", point.getCount());
        assertNull("getMin was non-null", point.getMin());
        assertNull("getMax was non-null", point.getMax());
        assertNull("getStdDev was non-null", point.getStdDev());
        assertTrue(data.getProperties().isEmpty());
    }
}
