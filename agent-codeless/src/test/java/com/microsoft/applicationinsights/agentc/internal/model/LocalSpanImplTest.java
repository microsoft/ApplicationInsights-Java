package com.microsoft.applicationinsights.agentc.internal.model;

import org.junit.*;

public class LocalSpanImplTest {

    @Test
    public void testParsing() {
        String telemetryName = LocalSpanImpl.getTelemetryName("__custom,xyz.ABC,abc");
        Assert.assertEquals("xyz/ABC.abc", telemetryName);
    }
}
