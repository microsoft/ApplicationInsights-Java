package com.microsoft.applicationinsights.telemetry;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public final class ExceptionTelemetryTest {

    @Test
    public void testSetException() {
        NullPointerException exception = new NullPointerException("mock");
        ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);

        NullPointerException exception1 = new NullPointerException("mock");
        exceptionTelemetry.setException(exception1);

        assertSame(exception1, exceptionTelemetry.getException());
    }

    @Test
    public void testCtor() {
        NullPointerException exception = new NullPointerException("mock");
        ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);

        assertSame(exception, exceptionTelemetry.getException());
        assertTrue(exceptionTelemetry.getProperties().isEmpty());
        assertTrue(exceptionTelemetry.getMetrics().isEmpty());
        assertEquals(exceptionTelemetry.getExceptions().size(), 1);
    }

    @Test
    public void testSetExceptionHandledAt() {
        NullPointerException exception = new NullPointerException("mock");
        ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);
        exceptionTelemetry.setExceptionHandledAt(ExceptionHandledAt.Platform);

        assertSame(ExceptionHandledAt.Platform, exceptionTelemetry.getExceptionHandledAt());
    }

    @Test
    public void testExceptions() {
        Exception exception = new IOException("mocka", new IllegalArgumentException("mockb"));
        ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);

        assertEquals(exceptionTelemetry.getExceptions().size(), 2);
    }
}