package com.microsoft.applicationinsights.smoketest;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import org.junit.*;

@UseAgent
public class SpringBootAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/delayedSystemExit")
    public void doDelayedSystemExitTest() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);
        mockedIngestion.waitForItem(input -> {
            if (!"MessageData".equals(input.getData().getBaseType())) {
                return false;
            }
            MessageData data = (MessageData) ((Data<?>) input.getData()).getBaseData();
            return data.getMessage().equals("this is an error right before shutdown");
        }, 10, TimeUnit.SECONDS);
    }
}
