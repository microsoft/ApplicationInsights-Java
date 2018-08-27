package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import org.junit.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class LocalForwarderTelemetryChannelTest {

    @Test
    public void testConstructor() {
        LocalForwarderTelemetryChannel ch = new LocalForwarderTelemetryChannel(
                "localhost",
                false,
                555,
                6);
        assertFalse("isDeveloperMode should be false", ch.isDeveloperMode());
        assertEquals(555, ch.getTelemetryBuffer().getMaxTelemetriesInBatch());
        assertEquals(6, ch.getTelemetryBuffer().getTransmitBufferTimeoutInSeconds());
        assertEquals("localhost", ch.getTransmitter().getEndpoint());
    }

    @Test
    public void testMapConstructor() {
        Map<String, String> args = new HashMap<String, String>() {{

        }};

        // TODO
    }

}
