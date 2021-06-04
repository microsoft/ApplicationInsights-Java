package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;
import static org.junit.Assert.*;

@UseAgent("telemetryprocessors")
public class SpringBootAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertEquals("testValue1", rd.getProperties().get("attribute1"));
        assertEquals("testValue2", rd.getProperties().get("attribute2"));
        assertEquals("sensitiveData1", rd.getProperties().get("sensitiveAttribute1"));
        assertEquals("/TelemetryProcessors/test", rd.getProperties().get("httpPath"));
        assertEquals(4, rd.getProperties().size());
        assertTrue(rd.getSuccess());
        // Log processor test
        List<MessageData> logs = mockedIngestion.getMessageDataInRequest();
        for(MessageData log:logs) {
            if(log.getProperties().get("LoggerName") != null && log.getProperties().get("LoggerName").equals("smoketestappcontroller")) {
                System.out.println(log.getMessage());
                assertEquals("smoketestappcontroller::INFO",log.getMessage());
            }
        }
    }

    @Test
    @TargetUri("/sensitivedata")
    public void doSimpleTestPiiData() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertEquals("testValue1::testValue2", rd.getName());
        assertEquals("testValue1", rd.getProperties().get("attribute1"));
        assertEquals("testValue2", rd.getProperties().get("attribute2"));
        assertEquals("redacted", rd.getProperties().get("sensitiveAttribute1"));
        assertEquals("/TelemetryProcessors/sensitivedata", rd.getProperties().get("httpPath"));
        assertEquals(4, rd.getProperties().size());
        assertTrue(rd.getSuccess());
    }
}
