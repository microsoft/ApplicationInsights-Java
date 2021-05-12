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
        List<Envelope> metrics = mockedIngestion.waitForItems(getMetricPredicate("Feature"), 1, 70, TimeUnit.SECONDS);

        MetricData data = (MetricData) ((Data<?>) metrics.get(0).getData()).getBaseData();
        assertNotNull(data.getProperties().get("rp"));
        assertNotNull(data.getProperties().get("attach"));
        assertNotNull(data.getProperties().get("cikey"));
        assertNotNull(data.getProperties().get("runtimeVersion"));
        assertNotNull(data.getProperties().get("os"));
        assertNotNull(data.getProperties().get("language"));
        assertNotNull(data.getProperties().get("version"));
        assertNotNull(data.getProperties().get("feature"));
        assertEquals(8, data.getProperties().size());

        List<Envelope> attachMetrics = mockedIngestion.waitForItems(getMetricPredicate("Attach"), 2, 70, TimeUnit.SECONDS);
        assertEquals(2, attachMetrics.size());

        MetricData attachData = (MetricData) ((Data<?>) attachMetrics.get(0).getData()).getBaseData();
        assertNotNull(attachData.getProperties().get("rp"));
        assertNotNull(attachData.getProperties().get("attach"));
        assertNotNull(attachData.getProperties().get("cikey"));
        assertNotNull(attachData.getProperties().get("runtimeVersion"));
        assertNotNull(attachData.getProperties().get("os"));
        assertNotNull(attachData.getProperties().get("language"));
        assertNotNull(attachData.getProperties().get("version"));
        assertNotNull(attachData.getProperties().get("rpId"));
        assertEquals(8, data.getProperties().size());

        List<Envelope> requestSuccessCountMetrics = mockedIngestion.waitForItems(getMetricPredicate("Request Success Count"), 2, 70, TimeUnit.SECONDS);
        assertEquals(2, requestSuccessCountMetrics.size());

        MetricData requestSuccessCountData = (MetricData) ((Data<?>) requestSuccessCountMetrics.get(0).getData()).getBaseData();
        assertNotNull(requestSuccessCountData.getProperties().get("rp"));
        assertNotNull(requestSuccessCountData.getProperties().get("attach"));
        assertNotNull(requestSuccessCountData.getProperties().get("cikey"));
        assertNotNull(requestSuccessCountData.getProperties().get("runtimeVersion"));
        assertNotNull(requestSuccessCountData.getProperties().get("os"));
        assertNotNull(requestSuccessCountData.getProperties().get("language"));
        assertNotNull(requestSuccessCountData.getProperties().get("version"));
        assertNotNull(requestSuccessCountData.getProperties().get("instrumentation"));
        assertEquals(8, data.getProperties().size());

        List<Envelope> requestDurationMetrics = mockedIngestion.waitForItems(getMetricPredicate("Request Duration"), 2, 70, TimeUnit.SECONDS);
        assertEquals(2, requestDurationMetrics.size());

        MetricData requestDurationData = (MetricData) ((Data<?>) requestDurationMetrics.get(0).getData()).getBaseData();
        assertNotNull(requestDurationData.getProperties().get("rp"));
        assertNotNull(requestDurationData.getProperties().get("attach"));
        assertNotNull(requestDurationData.getProperties().get("cikey"));
        assertNotNull(requestDurationData.getProperties().get("runtimeVersion"));
        assertNotNull(requestDurationData.getProperties().get("os"));
        assertNotNull(requestDurationData.getProperties().get("language"));
        assertNotNull(requestDurationData.getProperties().get("version"));
        assertNotNull(requestDurationData.getProperties().get("instrumentation"));
        assertEquals(8, data.getProperties().size());
    }

    private static Predicate<Envelope> getMetricPredicate(String name) {
        Preconditions.checkNotNull(name, "name");
        return new Predicate<Envelope>() {
            @Override
            public boolean apply(Envelope input) {
                if(!input.getData().getBaseType().equals("MetricData")) {
                    return false;
                }
                MetricData md = getBaseData(input);
                return name.equals(md.getMetrics().get(0).getName());
            }
        };
    }

}