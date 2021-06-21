package com.microsoft.applicationinsights.agent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryExceptionDetails;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionsTest {

    @Test
    public void test() {
        // given
        String str = toString(new IllegalStateException("test"));

        // when
        List<TelemetryExceptionDetails> list = Exceptions.fullParse(str);

        // then
        assertThat(list.size()).isEqualTo(1);

        TelemetryExceptionDetails details = list.get(0);
        assertThat(details.getTypeName()).isEqualTo(IllegalStateException.class.getName());
        assertThat(details.getMessage()).isEqualTo("test");
    }

    @Test
    public void testWithNoMessage() {
        // given
        String str = toString(new IllegalStateException());

        // when
        List<TelemetryExceptionDetails> list = Exceptions.fullParse(str);

        // then
        assertThat(list.size()).isEqualTo(1);

        TelemetryExceptionDetails details = list.get(0);
        assertThat(details.getTypeName()).isEqualTo(IllegalStateException.class.getName());
        assertThat(details.getMessage()).isNull();
    }

    @Test
    public void testWithCausedBy() {
        // given
        RuntimeException causedBy = new RuntimeException("the cause");
        String str = toString(new IllegalStateException("test", causedBy));

        // when
        List<TelemetryExceptionDetails> list = Exceptions.fullParse(str);

        // then
        assertThat(list.size()).isEqualTo(2);

        TelemetryExceptionDetails details = list.get(0);
        assertThat(details.getTypeName()).isEqualTo(IllegalStateException.class.getName());
        assertThat(details.getMessage()).isEqualTo("test");

        TelemetryExceptionDetails causedByDetails = list.get(1);
        assertThat(causedByDetails.getTypeName()).isEqualTo(RuntimeException.class.getName());
        assertThat(causedByDetails.getMessage()).isEqualTo("the cause");

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
        assertThat(list.size()).isEqualTo(1);

        TelemetryExceptionDetails details = list.get(0);
        assertThat(details.getTypeName()).isEqualTo(IllegalStateException.class.getName());
        assertThat(details.getMessage()).isEqualTo("test");
    }

    private static String toString(final Throwable t) {
        final StringWriter out = new StringWriter();
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
}
