package com.microsoft.applicationinsights.extensibility.initializer;

import com.microsoft.applicationinsights.telemetry.Telemetry;
import junit.framework.TestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Date;

import static org.mockito.Matchers.anyObject;

public final class TimestampPropertyInitializerTest extends TestCase {
    @Test
    public void testSetTimestampWithNull() {
        Telemetry telemetry = createMockTelemetryAndActivateInitializer(null);
        Mockito.verify(telemetry, Mockito.times(1)).setTimestamp((Date)anyObject());
    }

    @Test
    public void testSetTimestampWithNonNull() {
        Telemetry telemetry = createMockTelemetryAndActivateInitializer(new Date());
        Mockito.verify(telemetry, Mockito.never()).setTimestamp((Date)anyObject());
    }

    private static Telemetry createMockTelemetryAndActivateInitializer(Date mockValue) {
        Telemetry telemetry = Mockito.mock(Telemetry.class);
        Mockito.doReturn(mockValue).when(telemetry).getTimestamp();

        new TimestampPropertyInitializer().initialize(telemetry);

        return telemetry;
    }
}