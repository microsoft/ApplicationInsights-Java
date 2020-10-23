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
        mockedIngestion.waitForItems("RequestData", 1);
    }
}
