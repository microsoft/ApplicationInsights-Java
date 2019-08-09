package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.*;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

@UseAgent
@WithDependencyContainers(@DependencyContainer(value="redis", portMapping="6379"))
public class SampleTestWithDependencyContainer extends AiSmokeTest {

    // TODO FIXME this is a sample test for dependencies. it shouldn't count towards test coverage
    @Test
    @TargetUri("/index.jsp")
    public void doCalcSendsRequestDataAndMetricData() throws Exception {
        assertTrue("mocked ingestion has no data", mockedIngestion.hasData());
        assertTrue("mocked ingestion has 0 items", mockedIngestion.getItemCount() > 0);

        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        validateSdkName(d, "java-web-manual");
        validateSdkName(rdd, "ja-redis");
    }

    private void validateSdkName(Domain data, String sdkName) {
        Envelope envelope = mockedIngestion.getEnvelopeForBaseData(data);
        String sdkVersion = envelope.getTags().get("ai.internal.sdkVersion");
        assertThat(sdkVersion, startsWith(sdkName + ":"));
    }
}