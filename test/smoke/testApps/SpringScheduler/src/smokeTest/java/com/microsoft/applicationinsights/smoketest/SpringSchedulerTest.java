package com.microsoft.applicationinsights.smoketest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.google.common.base.Stopwatch;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent
public class SpringSchedulerTest extends AiSmokeTest {

    @Test
    @TargetUri("/scheduler")
    public void fixedRateSchedulerTest() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (mockedIngestion.getCountForType("RequestData") < 20 && stopwatch.elapsed(TimeUnit.SECONDS) < 10) {
        }

        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        assertTrue(rdList.size() >= 20);

        List<Envelope> httpRequestList = new ArrayList<>(1);
        List<Envelope> schedulerRequestList = new ArrayList<>();
        groupRequestList(httpRequestList, schedulerRequestList, rdList);
        assertEquals(1, httpRequestList.size());
        assertTrue(schedulerRequestList.size() >= 19);

        RequestData rd = (RequestData) ((Data) httpRequestList.get(0).getData()).getBaseData();
        assertEquals("GET /scheduler", rd.getName());

        for (Envelope envelope : schedulerRequestList) {
            assertEquals("SpringSchedulerApp.fixedRateScheduler", ((RequestData) ((Data) envelope.getData()).getBaseData()).getName());
        }
    }

    private void groupRequestList(List<Envelope> httpRequestList, List<Envelope> schedulerRequestList, List<Envelope> rdList) {
        for (Envelope envelope : rdList) {
            RequestData rd = (RequestData) ((Data) envelope.getData()).getBaseData();
            if (rd.getName().equals("GET /scheduler")) {
                httpRequestList.add(envelope);
            } else if (rd.getName().equals("SpringSchedulerApp.fixedRateScheduler")) {
                schedulerRequestList.add(envelope);
            }
        }
    }
}
