package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasName;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;

@UseAgent
public class JmsTest extends AiSmokeTest {

    @Test
    @TargetUri("/sendMessage")
    public void doMostBasicTest() throws Exception {
        mockedIngestion.waitForItems("RequestData", 2);
        mockedIngestion.waitForItemsInRequest("RemoteDependencyData", 1);

        List<RequestData> rdList = mockedIngestion.getTelemetryDataByType("RequestData");
        List<RemoteDependencyData> rddList = mockedIngestion.getTelemetryDataByType("RemoteDependencyData");

        RemoteDependencyData rdd = rddList.get(0);

        assertThat(rdList, hasItem(hasName("GET /sendMessage")));
        assertThat(rdList, hasItem(hasName("JMS Message: MessagingMessageListenerAdapter")));

        assertEquals(rdd.getName(), "JMS Send: queue://message");
    }
}
