package com.microsoft.applicationinsights.smoketest;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseAgent
public class SpringBootAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> httpRequestList = new ArrayList<>();
        for (Envelope envelope : rdList) {
            RequestData rd = (RequestData) ((Data) envelope.getData()).getBaseData();
            if (rd.getName().equals("GET /test")) {
                httpRequestList.add(envelope);
            }
        }
        assertEquals(1, httpRequestList.size());
    }
}
