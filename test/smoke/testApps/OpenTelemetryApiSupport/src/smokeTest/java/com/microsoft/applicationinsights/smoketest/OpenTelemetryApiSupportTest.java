package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent("opentelemetryapisupport")
public class OpenTelemetryApiSupportTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);

        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        // ideally want these on rd, but can't get SERVER span yet
        // see https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1726#issuecomment-731890267
        assertEquals("myvalue1", rdd.getProperties().get("myattr1"));
        assertEquals("myvalue2", rdd.getProperties().get("myattr2"));
        assertEquals("myuser", rddEnvelope.getTags().get("ai.user.id"));
        assertEquals("myspanname", rdd.getName());

        assertTrue(rd.getSuccess());
    }
}
