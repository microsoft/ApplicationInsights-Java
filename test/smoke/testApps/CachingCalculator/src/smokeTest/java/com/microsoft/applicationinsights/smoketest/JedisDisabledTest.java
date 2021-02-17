package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent("disabled_redis")
@WithDependencyContainers(@DependencyContainer(value="redis", portMapping="6379"))
public class JedisDisabledTest extends AiSmokeTest {

    @Test
    @TargetUri("/index.jsp")
    public void doCalcSendsRequestDataAndMetricData() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("/CachingCalculator/index.jsp", rd.getName());
        assertEquals("200", rd.getResponseCode());

        // sleep a bit and make sure no jedis dependencies are reported
        Thread.sleep(5000);
        assertEquals(0, mockedIngestion.getCountForType("RemoteDependencyData"));
   }
}
