package com.microsoft.applicationinsights.smoketest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.google.common.base.Stopwatch;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent
public class SpringBootAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        boolean found = false;
        for (Envelope envelope : rdList) {
            RequestData rd = (RequestData) ((Data) envelope.getData()).getBaseData();
            if (rd.getName().equals("GET /test")) {
                found = true;
            }
        }

        assertTrue(found);
    }

    @Test
    @TargetUri("/scheduler")
    public void fixedRateSchedulerTest() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (mockedIngestion.getCountForType("RequestData") < 20 && stopwatch.elapsed(TimeUnit.SECONDS) < 10) {
        }

        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertTrue(rdList.size() >= 20);
        for (int i = 0; i < rdList.size(); i++) {
            Envelope envelope = rdList.get(i);
            String operationName = envelope.getTags().get("ai.operation.name");
            RequestData rd = (RequestData) ((Data) envelope.getData()).getBaseData();
            if (operationName.equals("SpringBootApp.fixedRateScheduler") && rd.getName().equals("SpringBootApp.fixedRateScheduler")) {
                Envelope rddEnvelope = findRemoteDependencyData(rd.getId(), new ArrayList<>(rddList));
                if (rddEnvelope != null) {
                    RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();
                    assertEquals("GET /search", rdd.getName());
                    assertEquals("www.bing.com", rdd.getTarget());
                    assertEquals(rd.getId(), rddEnvelope.getTags().get("ai.operation.parentId"));
                }
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
}
