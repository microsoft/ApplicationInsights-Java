package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;

import org.junit.Test;

public class HeartBeatTest extends AiSmokeTest {

    @Test
    @TargetUri(value = "/index.jsp")
    public void testHeartBeat() throws Exception {
        List<Envelope> metrics = mockedIngestion.waitForItems(getMetricPredicate("HeartbeatState"), 2, 70, TimeUnit.SECONDS);
        assertEquals(2, metrics.size());
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