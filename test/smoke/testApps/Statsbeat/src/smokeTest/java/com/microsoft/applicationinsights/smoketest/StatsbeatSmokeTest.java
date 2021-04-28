package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;

import org.junit.Test;

@UseAgent("faststatsbeat")
public class StatsbeatSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri(value = "/index.jsp")
    public void testStatsbeat() throws Exception {
        List<Envelope> metrics = mockedIngestion.waitForItems(getMetricPredicate("Request Success Count"), 4, 120, TimeUnit.SECONDS);
        assertEquals(4, metrics.size());

        MetricData data = (MetricData) ((Data<?>) metrics.get(0).getData()).getBaseData();
        assertNotNull(data.getProperties().get("rp"));
        assertNotNull(data.getProperties().get("attach"));
        assertNotNull(data.getProperties().get("cikey"));
        assertNotNull(data.getProperties().get("runtimeVersion"));
        assertNotNull(data.getProperties().get("os"));
        assertNotNull(data.getProperties().get("language"));
        assertNotNull(data.getProperties().get("version"));
        assertNotNull(data.getProperties().get("instrumentation"));
        assertEquals(8, data.getProperties().size());
    }

    private static Predicate<Envelope> getMetricPredicate(String name) {
        Preconditions.checkNotNull(name, "name");
        return new Predicate<Envelope>() {
            @Override
            public boolean apply(@Nullable Envelope input) {
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