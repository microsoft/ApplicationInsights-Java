package com.microsoft.applicationinsights.internal.quickpulse;

import com.azure.monitor.opentelemetry.exporter.implementation.models.RemoteDependencyData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.RequestData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryExceptionData;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector.CountAndDuration;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector.Counters;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector.FinalCounters;
import org.junit.*;

import java.util.Date;

import static com.microsoft.applicationinsights.TelemetryUtil.*;
import static org.junit.Assert.*;

public class QuickPulseDataCollectorTests {

    private static final String FAKE_INSTRUMENTATION_KEY = "fake-instrumentation-key";

    @Before
    public void setup() {
        QuickPulseDataCollector.INSTANCE.disable();
    }

    @After
    public void tearDown() {
        QuickPulseDataCollector.INSTANCE.disable();
    }

    @Test
    public void initialStateIsDisabled() {
        assertNull(QuickPulseDataCollector.INSTANCE.peek());
    }

    @Test
    public void emptyCountsAndDurationsAfterEnable() {
        QuickPulseDataCollector.INSTANCE.enable(FAKE_INSTRUMENTATION_KEY);
        final FinalCounters counters = QuickPulseDataCollector.INSTANCE.peek();
        assertCountersReset(counters);
    }

    @Test
    public void nullCountersAfterDisable() {
        QuickPulseDataCollector.INSTANCE.enable(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.disable();
        assertNull(QuickPulseDataCollector.INSTANCE.peek());
    }

    @Test
    public void requestTelemetryIsCounted_DurationIsSum() {
        QuickPulseDataCollector.INSTANCE.enable(FAKE_INSTRUMENTATION_KEY);

        // add a success and peek
        final long duration = 112233L;
        RequestData data = createRequestData("request-test", new Date(), duration, "200", true);
        QuickPulseDataCollector.INSTANCE.add(createTelemetry(data, FAKE_INSTRUMENTATION_KEY));
        FinalCounters counters = QuickPulseDataCollector.INSTANCE.peek();
        assertEquals(1, counters.requests);
        assertEquals(0, counters.unsuccessfulRequests);
        assertEquals((double)duration, counters.requestsDuration, Math.ulp((double)duration));

        // add another success and peek
        final long duration2 = 65421L;
        data = createRequestData("request-test-2", new Date(), duration2, "200", true);
        // FIXME (trask) how to set ikey
        // rt.getContext().setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(data);
        counters = QuickPulseDataCollector.INSTANCE.peek();
        double total = duration + duration2;
        assertEquals(2, counters.requests);
        assertEquals(0, counters.unsuccessfulRequests);
        assertEquals(total, counters.requestsDuration, Math.ulp(total));

        // add a failure and get/reset
        final long duration3 = 9988L;
        data = createRequestData("request-test-3", new Date(), duration3, "400", false);
        // FIXME (trask) how to set ikey
        // rt.getContext().setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(data);
        counters = QuickPulseDataCollector.INSTANCE.getAndRestart();
        total += duration3;
        assertEquals(3, counters.requests);
        assertEquals(1, counters.unsuccessfulRequests);
        assertEquals(total, counters.requestsDuration, Math.ulp(total));

        assertCountersReset(QuickPulseDataCollector.INSTANCE.peek());
    }

    @Test
    public void dependencyTelemetryIsCounted_DurationIsSum() {
        QuickPulseDataCollector.INSTANCE.enable(FAKE_INSTRUMENTATION_KEY);

        // add a success and peek.
        final long duration = 112233L;
        RemoteDependencyData rdt = createRemoteDependencyData("dep-test", "dep-test-cmd", duration, true);
        // FIXME (trask) how to set ikey
        // rdt.getContext().setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(rdt);
        FinalCounters counters = QuickPulseDataCollector.INSTANCE.peek();
        assertEquals(1, counters.rdds);
        assertEquals(0, counters.unsuccessfulRdds);
        assertEquals((double)duration, counters.rddsDuration, Math.ulp((double)duration));

        // add another success and peek.
        final long duration2 = 334455L;
        rdt = createRemoteDependencyData("dep-test-2", "dep-test-cmd-2", duration2, true);
        // FIXME (trask) how to set ikey
        // rdt.getContext().setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(rdt);
        counters = QuickPulseDataCollector.INSTANCE.peek();
        assertEquals(2, counters.rdds);
        assertEquals(0, counters.unsuccessfulRdds);
        double total = duration + duration2;
        assertEquals(total, counters.rddsDuration, Math.ulp(total));

        // add a failure and get/reset.
        final long duration3 = 123456L;
        rdt = createRemoteDependencyData("dep-test-3", "dep-test-cmd-3", duration3, false);
        // FIXME (trask) how to set ikey
        // rdt.getContext().setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(rdt);
        counters = QuickPulseDataCollector.INSTANCE.getAndRestart();
        assertEquals(3, counters.rdds);
        assertEquals(1, counters.unsuccessfulRdds);
        total += duration3;
        assertEquals(total, counters.rddsDuration, Math.ulp(total));

        assertCountersReset(QuickPulseDataCollector.INSTANCE.peek());
    }

    @Test
    public void exceptionTelemetryIsCounted() {
        QuickPulseDataCollector.INSTANCE.enable(FAKE_INSTRUMENTATION_KEY);

        TelemetryExceptionData et = new ExceptionTelemetry(new Exception());
        // FIXME (trask) how to set ikey
        // et.getContext().setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(et);
        FinalCounters counters = QuickPulseDataCollector.INSTANCE.peek();
        assertEquals(1, counters.exceptions, Math.ulp(1.0));

        et = new ExceptionTelemetry(new Exception());
        // FIXME (trask) how to set ikey
        // et.getContext().setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(et);
        counters = QuickPulseDataCollector.INSTANCE.getAndRestart();
        assertEquals(2, counters.exceptions, Math.ulp(2.0));

        assertCountersReset(QuickPulseDataCollector.INSTANCE.peek());
    }

    @Test
    public void encodeDecodeIsIdentity() {
        final long count = 456L;
        final long duration = 112233L;
        final long encoded = Counters.encodeCountAndDuration(count, duration);
        final CountAndDuration inputs = Counters.decodeCountAndDuration(encoded);
        assertEquals(count, inputs.count);
        assertEquals(duration, inputs.duration);
    }

    private void assertCountersReset(FinalCounters counters) {
        assertNotNull(counters);

        assertEquals(0, counters.rdds);
        assertEquals(0.0, counters.rddsDuration, Math.ulp(0.0));
        assertEquals(0, counters.unsuccessfulRdds);

        assertEquals(0, counters.requests);
        assertEquals(0.0, counters.requestsDuration, Math.ulp(0.0));
        assertEquals(0, counters.unsuccessfulRequests);

        // FIXME exceptions is stored as a double but counted as an int; is that correct?
        assertEquals(0, (int) counters.exceptions);
    }
}
