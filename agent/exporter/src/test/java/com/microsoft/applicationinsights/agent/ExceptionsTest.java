package com.microsoft.applicationinsights.agent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryExceptionDetails;
import org.junit.*;

import static org.junit.Assert.*;

public class ExceptionsTest {

    @Test
    public void test() {
        // given
        String str = toString(new IllegalStateException("test"));

        // when
        List<TelemetryExceptionDetails> list = Exceptions.fullParse(str);

        // then
        assertEquals(1, list.size());

        TelemetryExceptionDetails details = list.get(0);
        assertEquals(IllegalStateException.class.getName(), details.getTypeName());
        assertEquals("test", details.getMessage());
    }

    @Test
    public void testWithNoMessage() {
        // given
        String str = toString(new IllegalStateException());

        // when
        List<TelemetryExceptionDetails> list = Exceptions.fullParse(str);

        // then
        assertEquals(1, list.size());

        TelemetryExceptionDetails details = list.get(0);
        assertEquals(IllegalStateException.class.getName(), details.getTypeName());
        assertNull(details.getMessage());
    }

    @Test
    public void testWithCausedBy() {
        // given
        RuntimeException causedBy = new RuntimeException("the cause");
        String str = toString(new IllegalStateException("test", causedBy));

        // when
        List<TelemetryExceptionDetails> list = Exceptions.fullParse(str);

        // then
        assertEquals(2, list.size());

        TelemetryExceptionDetails details = list.get(0);
        assertEquals(IllegalStateException.class.getName(), details.getTypeName());
        assertEquals("test", details.getMessage());

        TelemetryExceptionDetails causedByDetails = list.get(1);
        assertEquals(RuntimeException.class.getName(), causedByDetails.getTypeName());
        assertEquals("the cause", causedByDetails.getMessage());

    }

    @Test
    public void shouldIgnoreSuppressed() {
        // given
        RuntimeException suppressed = new RuntimeException("the suppressed");
        IllegalStateException exception = new IllegalStateException("test");
        exception.addSuppressed(suppressed);
        String str = toString(exception);

        // when
        List<TelemetryExceptionDetails> list = Exceptions.fullParse(str);

        // then
        assertEquals(1, list.size());

        TelemetryExceptionDetails details = list.get(0);
        assertEquals(IllegalStateException.class.getName(), details.getTypeName());
        assertEquals("test", details.getMessage());
    }

    private static String toString(final Throwable t) {
        final StringWriter out = new StringWriter();
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
}
