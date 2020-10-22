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
        assertTrue(schedulerRequestList.size() >= 20);

        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        for (Envelope envelope : httpRequestList) {
            RequestData rd = (RequestData) ((Data) envelope.getData()).getBaseData();
            assertEquals("GET /scheduler", rd.getName());
            Envelope rddEnvelope = findRemoteDependencyData(rd.getId(), new ArrayList<>(rddList));
            if (rddEnvelope != null) {
                RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();
                assertEquals("TestController.scheduler", rdd.getName());
                assertEquals(rd.getId(), rddEnvelope.getTags().get("ai.operation.parentId"));
            }
        }

        for (Envelope envelope : schedulerRequestList) {
            RequestData rd = (RequestData) ((Data) envelope.getData()).getBaseData();
            assertEquals("SpringBootApp.fixedRateScheduler", rd.getName());
            Envelope rddEnvelope = findRemoteDependencyData(rd.getId(), new ArrayList<>(rddList));
            if (rddEnvelope != null) {
                RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();
                assertEquals("GET /search", rdd.getName());
                assertEquals("www.bing.com", rdd.getTarget());
                assertEquals(rd.getId(), rddEnvelope.getTags().get("ai.operation.parentId"));
            }
        }
    }

    private Envelope findRemoteDependencyData(String parentId, List<Envelope> rddList) {
        for (Envelope envelope : rddList) {
            if (envelope.getTags().get("ai.operation.parentId").equals(parentId)) {
                return envelope;
            }
        }

        return null;
    }

    private void groupRequestList(List<Envelope> httpRequestList, List<Envelope> schedulerRequestList, List<Envelope> rdList) {
        if (httpRequestList == null || schedulerRequestList == null || rdList == null) {
            return;
        }

        for (Envelope envelope : rdList) {
            RequestData rd = (RequestData) ((Data) envelope.getData()).getBaseData();
            if (rd.getName().equals("GET /scheduler")) {
                httpRequestList.add(envelope);
            } else if (rd.getName().equals("SpringBootApp.fixedRateScheduler")) {
                schedulerRequestList.add(envelope);
            }
        }
    }
}