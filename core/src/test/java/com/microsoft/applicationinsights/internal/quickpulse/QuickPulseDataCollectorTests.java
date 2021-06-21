package com.microsoft.applicationinsights.internal.quickpulse;

import com.azure.monitor.opentelemetry.exporter.implementation.models.RemoteDependencyData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.RequestData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryExceptionData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.FormattedDuration;
import com.microsoft.applicationinsights.FormattedTime;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector.CountAndDuration;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector.Counters;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector.FinalCounters;
import org.junit.jupiter.api.*;

import java.util.Date;

import static com.microsoft.applicationinsights.TelemetryUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

class QuickPulseDataCollectorTests {

    private static final String FAKE_INSTRUMENTATION_KEY = "fake-instrumentation-key";

    @BeforeEach
    void setup() {
        QuickPulseDataCollector.INSTANCE.disable();
    }

    @AfterEach
    void tearDown() {
        QuickPulseDataCollector.INSTANCE.disable();
    }

    @Test
    void initialStateIsDisabled() {
        assertThat(QuickPulseDataCollector.INSTANCE.peek()).isNull();
    }

    @Test
    void emptyCountsAndDurationsAfterEnable() {
        TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.enable(telemetryClient);
        FinalCounters counters = QuickPulseDataCollector.INSTANCE.peek();
        assertCountersReset(counters);
    }

    @Test
    void nullCountersAfterDisable() {
        TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.enable(telemetryClient);
        QuickPulseDataCollector.INSTANCE.disable();
        assertThat(QuickPulseDataCollector.INSTANCE.peek()).isNull();
    }

    @Test
    void requestTelemetryIsCounted_DurationIsSum() {
        TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.enable(telemetryClient);

        // add a success and peek
        final long duration = 112233L;
        TelemetryItem telemetry = createRequestTelemetry("request-test", new Date(), duration, "200", true);
        telemetry.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(telemetry);
        FinalCounters counters = QuickPulseDataCollector.INSTANCE.peek();
        assertThat(counters.requests).isEqualTo(1);
        assertThat(counters.unsuccessfulRequests).isEqualTo(0);
        assertThat(counters.requestsDuration).isEqualTo(duration);

        // add another success and peek
        final long duration2 = 65421L;
        telemetry = createRequestTelemetry("request-test-2", new Date(), duration2, "200", true);
        telemetry.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(telemetry);
        counters = QuickPulseDataCollector.INSTANCE.peek();
        double total = duration + duration2;
        assertThat(counters.requests).isEqualTo(2);
        assertThat(counters.unsuccessfulRequests).isEqualTo(0);
        assertThat(counters.requestsDuration).isEqualTo(total);

        // add a failure and get/reset
        final long duration3 = 9988L;
        telemetry = createRequestTelemetry("request-test-3", new Date(), duration3, "400", false);
        telemetry.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(telemetry);
        counters = QuickPulseDataCollector.INSTANCE.getAndRestart();
        total += duration3;
        assertThat(counters.requests).isEqualTo(3);
        assertThat(counters.unsuccessfulRequests).isEqualTo(1);
        assertThat(counters.requestsDuration).isEqualTo(total);

        assertCountersReset(QuickPulseDataCollector.INSTANCE.peek());
    }

    @Test
    void dependencyTelemetryIsCounted_DurationIsSum() {
        TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.enable(telemetryClient);

        // add a success and peek.
        final long duration = 112233L;
        TelemetryItem telemetry = createRemoteDependencyTelemetry("dep-test", "dep-test-cmd", duration, true);
        telemetry.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(telemetry);
        FinalCounters counters = QuickPulseDataCollector.INSTANCE.peek();
        assertThat(counters.rdds).isEqualTo(1);
        assertThat(counters.unsuccessfulRdds).isEqualTo(0);
        assertThat(counters.rddsDuration).isEqualTo(duration);

        // add another success and peek.
        final long duration2 = 334455L;
        telemetry = createRemoteDependencyTelemetry("dep-test-2", "dep-test-cmd-2", duration2, true);
        telemetry.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(telemetry);
        counters = QuickPulseDataCollector.INSTANCE.peek();
        assertThat(counters.rdds).isEqualTo(2);
        assertThat(counters.unsuccessfulRdds).isEqualTo(0);
        double total = duration + duration2;
        assertThat(counters.rddsDuration).isEqualTo(total);

        // add a failure and get/reset.
        final long duration3 = 123456L;
        telemetry = createRemoteDependencyTelemetry("dep-test-3", "dep-test-cmd-3", duration3, false);
        telemetry.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(telemetry);
        counters = QuickPulseDataCollector.INSTANCE.getAndRestart();
        assertThat(counters.rdds).isEqualTo(3);
        assertThat(counters.unsuccessfulRdds).isEqualTo(1);
        total += duration3;
        assertThat(counters.rddsDuration).isEqualTo(total);

        assertCountersReset(QuickPulseDataCollector.INSTANCE.peek());
    }

    @Test
    void exceptionTelemetryIsCounted() {
        TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.enable(telemetryClient);

        TelemetryItem telemetry = createExceptionTelemetry(new Exception());
        telemetry.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(telemetry);
        FinalCounters counters = QuickPulseDataCollector.INSTANCE.peek();
        assertThat(counters.exceptions).isEqualTo(1);

        telemetry = createExceptionTelemetry(new Exception());
        telemetry.setInstrumentationKey(FAKE_INSTRUMENTATION_KEY);
        QuickPulseDataCollector.INSTANCE.add(telemetry);
        counters = QuickPulseDataCollector.INSTANCE.getAndRestart();
        assertThat(counters.exceptions).isEqualTo(2);

        assertCountersReset(QuickPulseDataCollector.INSTANCE.peek());
    }

    @Test
    void encodeDecodeIsIdentity() {
        final long count = 456L;
        final long duration = 112233L;
        long encoded = Counters.encodeCountAndDuration(count, duration);
        CountAndDuration inputs = Counters.decodeCountAndDuration(encoded);
        assertThat(inputs.count).isEqualTo(count);
        assertThat(inputs.duration).isEqualTo(duration);
    }

    private static TelemetryItem createRequestTelemetry(String name, Date timestamp, long durationMillis, String responseCode, boolean success) {
        TelemetryItem telemetry = new TelemetryItem();
        RequestData data = new RequestData();
        new TelemetryClient().initRequestTelemetry(telemetry, data);

        data.setName(name);
        data.setDuration(FormattedDuration.fromMillis(durationMillis));
        data.setResponseCode(responseCode);
        data.setSuccess(success);

        telemetry.setTime(FormattedTime.fromDate(timestamp));
        return telemetry;
    }

    private static TelemetryItem createRemoteDependencyTelemetry(String name, String command, long durationMillis, boolean success) {
        TelemetryItem telemetry = new TelemetryItem();
        RemoteDependencyData data = new RemoteDependencyData();
        new TelemetryClient().initRemoteDependencyTelemetry(telemetry, data);

        data.setName(name);
        data.setData(command);
        data.setDuration(FormattedDuration.fromMillis(durationMillis));
        data.setSuccess(success);

        return telemetry;
    }

    private static TelemetryItem createExceptionTelemetry(Exception exception) {
        TelemetryItem telemetry = new TelemetryItem();
        TelemetryExceptionData data = new TelemetryExceptionData();
        new TelemetryClient().initExceptionTelemetry(telemetry, data);

        data.setExceptions(getExceptions(exception));

        return telemetry;
    }

    private static void assertCountersReset(FinalCounters counters) {
        assertThat(counters).isNotNull();

        assertThat(counters.rdds).isEqualTo(0);
        assertThat(counters.rddsDuration).isEqualTo(0);
        assertThat(counters.unsuccessfulRdds).isEqualTo(0);

        assertThat(counters.requests).isEqualTo(0);
        assertThat(counters.requestsDuration).isEqualTo(0);
        assertThat(counters.unsuccessfulRequests).isEqualTo(0);

        assertThat(counters.exceptions).isEqualTo(0);
    }
}
