package com.microsoft.applicationinsights.internal.channel.inprocess;

import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import org.junit.Test;

import java.util.HashMap;

public class InProcessTelemetryChannelTest {

    private final static String NON_VALID_URL = "http:sd{@~fsd.s.d.f;fffff";

    @Test(expected = IllegalArgumentException.class)
    public void testNotValidEndpointAddress() {
        new InProcessTelemetryChannel(NON_VALID_URL, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotValidEndpointAddressAsMapValue() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("EndpointAddress", NON_VALID_URL);
        new InProcessTelemetryChannel(map);
    }
}