package com.microsoft.applicationinsights.smoketest;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

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
            public boolean test(Envelope input) {
                if (!input.getData().getBaseType().equals("MetricData")) {
                    return false;
                }
                MetricData data = (MetricData) ((Data<?>) input.getData()).getBaseData();
                if (!"/test".equals(data.getProperties().get("uri"))) {
                    return false;
                }
                for (DataPoint point : data.getMetrics()) {
                    if (point.getName().equals("http_server_requests") && point.getCount() == 1) {
                        return true;
                    }
                }
                return false;
            }
        }, 1, 10, TimeUnit.SECONDS);

        MetricData data = (MetricData) ((Data<?>) metricItems.get(0).getData()).getBaseData();
        List<DataPoint> points = data.getMetrics();
        assertEquals(1, points.size());

        DataPoint point = points.get(0);

        assertEquals(DataPointType.Aggregation, point.getKind());
        assertEquals(1, point.getCount(), 0); // (this was verified above in Predicate also)
        assertEquals("http_server_requests", point.getName()); // (this was verified above in Predicate also)
        assertNull("getMin was non-null", point.getMin()); // this isn't desired, but see https://github.com/micrometer-metrics/micrometer/issues/457
        assertNotNull("getMax was null", point.getMax());
        assertNull("getStdDev was non-null", point.getStdDev());
    }
}
