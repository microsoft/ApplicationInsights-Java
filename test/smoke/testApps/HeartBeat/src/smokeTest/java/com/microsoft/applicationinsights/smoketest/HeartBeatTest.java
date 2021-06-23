package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;

import org.junit.Test;

@UseAgent("fastheartbeat")
public class HeartBeatTest extends AiSmokeTest {

    @Test
    @TargetUri(value = "/index.jsp")
    public void testHeartBeat() throws Exception {
        List<Envelope> metrics = mockedIngestion.waitForItems(getMetricPredicate("HeartbeatState"), 2, 70, TimeUnit.SECONDS);
        assertEquals(2, metrics.size());

        MetricData data = (MetricData) ((Data<?>) metrics.get(0).getData()).getBaseData();
        assertNotNull(data.getProperties().get("jreVersion"));
        assertNotNull(data.getProperties().get("sdkVersion"));
        assertNotNull(data.getProperties().get("osVersion"));
        assertNotNull(data.getProperties().get("processSessionId"));
        assertEquals(4, data.getProperties().size());
    }

    private static Predicate<Envelope> getMetricPredicate(String name) {
        Objects.requireNonNull(name, "name");
        return new Predicate<Envelope>() {
            @Override
            public boolean test(Envelope input) {
                if(input == null){
                    return false;
                }
                if(!input.getData().getBaseType().equals("MetricData")) {
                    return false;
                }
                MetricData md = getBaseData(input);
                return name.equals(md.getMetrics().get(0).getName());
            }
        };
    }
}
