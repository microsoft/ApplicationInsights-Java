package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent
public class SpringBootAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/delayedSystemExit")
    public void doDelayedSystemExitTest() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);
        mockedIngestion.waitForItems("MessageData", 1);

        List<MessageData> messageData = mockedIngestion.getTelemetryDataByType("MessageData");

        assertEquals(1, messageData.size());
        assertEquals("this is an error right before shutdown", messageData.get(0).getMessage());
    }
}
