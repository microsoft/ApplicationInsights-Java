package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.microsoft.applicationinsights.channel.concrete.ATelemetryChannel;
import com.microsoft.applicationinsights.internal.channel.common.TelemetryBuffer;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LocalForwarderTelemetryChannelTest {

    private LocalForwarderTelemetryChannel channel;
    private TelemetryBuffer<Telemetry> mockBuffer;

    @Before
    public void setup() {
        channel = new LocalForwarderTelemetryChannel("fake.local", false, 10, 10);
        mockBuffer = mock(TelemetryBuffer.class);
        channel.setTelemetryBuffer(mockBuffer);
    }

    @After
    public void tearDown() {
        channel.stop(10, TimeUnit.SECONDS);
        channel = null;
        reset(mockBuffer);
    }

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
    }

    @Test
    public void testMapConstructor() {
        Map<String, String> args = new HashMap<String, String>() {{
            put(ATelemetryChannel.DEVELOPER_MODE_NAME, "false");
            put(ATelemetryChannel.FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME, "7");
            put(ATelemetryChannel.MAX_TELEMETRY_BUFFER_CAPACITY_NAME, "789");
            put(ATelemetryChannel.ENDPOINT_ADDRESS_NAME, "myhost.local");
        }};

        LocalForwarderTelemetryChannel ch = new LocalForwarderTelemetryChannel(args);

        assertFalse("isDeveloperMode shoudl be false", ch.isDeveloperMode());
        assertEquals(789, ch.getTelemetryBuffer().getMaxTelemetriesInBatch());
        assertEquals(7, ch.getTelemetryBuffer().getTransmitBufferTimeoutInSeconds());
    }

    @Test
    public void sendAddsItemToBuffer() {
        final MetricTelemetry telemetry = new MetricTelemetry();
        telemetry.getContext().setInstrumentationKey("fake-ikey");

        channel.send(telemetry);
        verify(mockBuffer, times(1)).add(any(Telemetry.class));
    }

    @Test
    public void flushDelegatesToBuffer() {
        channel.flush();
        verify(mockBuffer, times(1)).flush();
    }

    @Test
    public void developerModeSetsBufferSizeToOne() {
        channel.setDeveloperMode(true);
        verify(mockBuffer, times(1)).setMaxTelemetriesInBatch(1);
    }

    @Test
    public void stopDelegatesToTransmitter() {
        final LocalForwarderTelemetriesTransmitter spy = spy(channel.getTransmitter());
        channel.setTransmitter(spy);

        channel.stop(10, TimeUnit.SECONDS);
        verify(spy, times(1)).stop(10, TimeUnit.SECONDS);
    }
}
