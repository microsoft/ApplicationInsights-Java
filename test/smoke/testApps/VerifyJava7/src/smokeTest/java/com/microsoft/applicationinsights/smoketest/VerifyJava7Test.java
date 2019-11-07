package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

@UseAgent
public class VerifyJava7Test extends AiSmokeTest {

    @Test
    @TargetUri("/verifyJava7")
    public void verifyJava7() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
    }
}
