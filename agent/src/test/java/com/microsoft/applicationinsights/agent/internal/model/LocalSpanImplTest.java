package com.microsoft.applicationinsights.agent.internal.model;

import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge.RemoteDependencyTelemetry;
import org.junit.Assert;
import org.junit.Test;

public class LocalSpanImplTest {

    @Test
    public void testParsing() {
        RemoteDependencyTelemetry telemetry =
                LocalSpanImpl.createRemoteDependencyTelemetry("__custom,xyz.ABC,abc,0,QRS", 0, null);
        Assert.assertEquals(telemetry.getName(), "xyz/ABC.abc");
        Assert.assertEquals(telemetry.getType(), "QRS");
    }

    @Test
    public void testParsingWithWeirdType() {
        RemoteDependencyTelemetry telemetry =
                LocalSpanImpl.createRemoteDependencyTelemetry("__custom,xyz.ABC,abc,0,QRS,TUV", 0, null);
        Assert.assertEquals(telemetry.getName(), "xyz/ABC.abc");
        Assert.assertEquals(telemetry.getType(), "QRS,TUV");
    }
}
