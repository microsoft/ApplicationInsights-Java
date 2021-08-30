/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.quickpulse;

import static com.microsoft.applicationinsights.agent.internal.quickpulse.QuickPulseTestBase.createExceptionTelemetry;
import static com.microsoft.applicationinsights.agent.internal.quickpulse.QuickPulseTestBase.createRemoteDependencyTelemetry;
import static com.microsoft.applicationinsights.agent.internal.quickpulse.QuickPulseTestBase.createRequestTelemetry;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.quickpulse.QuickPulseDataCollector.CountAndDuration;
import com.microsoft.applicationinsights.agent.internal.quickpulse.QuickPulseDataCollector.Counters;
import com.microsoft.applicationinsights.agent.internal.quickpulse.QuickPulseDataCollector.FinalCounters;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.time.Duration;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    TelemetryItem telemetry =
        createRequestTelemetry("request-test", new Date(), duration, "200", true);
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
    TelemetryItem telemetry =
        createRemoteDependencyTelemetry("dep-test", "dep-test-cmd", duration, true);
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

  @Test
  void parseDurations() {
    assertThat(QuickPulseDataCollector.parseDurationToMillis("00:00:00.123456")).isEqualTo(123);
    // current behavior rounds down (not sure if that's good or not?)
    assertThat(QuickPulseDataCollector.parseDurationToMillis("00:00:00.123999")).isEqualTo(123);
    assertThat(QuickPulseDataCollector.parseDurationToMillis("00:00:01.123456"))
        .isEqualTo(Duration.ofSeconds(1).plusMillis(123).toMillis());
    assertThat(QuickPulseDataCollector.parseDurationToMillis("00:00:12.123456"))
        .isEqualTo(Duration.ofSeconds(12).plusMillis(123).toMillis());
    assertThat(QuickPulseDataCollector.parseDurationToMillis("00:01:23.123456"))
        .isEqualTo(Duration.ofMinutes(1).plusSeconds(23).plusMillis(123).toMillis());
    assertThat(QuickPulseDataCollector.parseDurationToMillis("00:12:34.123456"))
        .isEqualTo(Duration.ofMinutes(12).plusSeconds(34).plusMillis(123).toMillis());
    assertThat(QuickPulseDataCollector.parseDurationToMillis("01:23:45.123456"))
        .isEqualTo(Duration.ofHours(1).plusMinutes(23).plusSeconds(45).plusMillis(123).toMillis());
    assertThat(QuickPulseDataCollector.parseDurationToMillis("12:34:56.123456"))
        .isEqualTo(Duration.ofHours(12).plusMinutes(34).plusSeconds(56).plusMillis(123).toMillis());
    assertThat(QuickPulseDataCollector.parseDurationToMillis("1.22:33:44.123456"))
        .isEqualTo(
            Duration.ofDays(1)
                .plusHours(22)
                .plusMinutes(33)
                .plusSeconds(44)
                .plusMillis(123)
                .toMillis());
    assertThat(QuickPulseDataCollector.parseDurationToMillis("11.22:33:44.123456"))
        .isEqualTo(
            Duration.ofDays(11)
                .plusHours(22)
                .plusMinutes(33)
                .plusSeconds(44)
                .plusMillis(123)
                .toMillis());
    assertThat(QuickPulseDataCollector.parseDurationToMillis("111.22:33:44.123456"))
        .isEqualTo(
            Duration.ofDays(111)
                .plusHours(22)
                .plusMinutes(33)
                .plusSeconds(44)
                .plusMillis(123)
                .toMillis());
    assertThat(QuickPulseDataCollector.parseDurationToMillis("1111.22:33:44.123456"))
        .isEqualTo(
            Duration.ofDays(1111)
                .plusHours(22)
                .plusMinutes(33)
                .plusSeconds(44)
                .plusMillis(123)
                .toMillis());
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
