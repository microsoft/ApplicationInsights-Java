// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.bootstrap.MicrometerUtil;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.javaagent.instrumentation.micrometer.ai.AzureMonitorMeterRegistry;
import io.opentelemetry.javaagent.instrumentation.micrometer.ai.AzureMonitorRegistryConfig;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MicrometerTest {

  private static final AgentTestingMicrometerDelegate delegate =
      new AgentTestingMicrometerDelegate();

  static {
    MicrometerUtil.setDelegate(delegate);
  }

  @BeforeAll
  static void applyInstrumentation() {
    // loading Metrics triggers the agent's injection of AzureMonitorMeterRegistry
    Metrics.globalRegistry.getRegistries();
  }

  @BeforeEach
  void resetMeasurements() {
    delegate.reset();
  }

  @Test
  void shouldRegisterAzureMonitorMeterRegistry() {
    assertThat(Metrics.globalRegistry.getRegistries()).hasSize(1);
    assertThat(Metrics.globalRegistry.getRegistries().iterator().next().getClass())
        .isEqualTo(AzureMonitorMeterRegistry.class);
  }

  @Test
  void shouldNotDoubleRegisterAzureMonitorMeterRegistry() {
    // when
    Metrics.addRegistry(
        new io.micrometer.azuremonitor.AzureMonitorMeterRegistry(
            key -> key.equals("azuremonitor.instrumentationKey") ? "0000" : null, Clock.SYSTEM));

    // then
    assertThat(Metrics.globalRegistry.getRegistries()).hasSize(1);
    assertThat(Metrics.globalRegistry.getRegistries().iterator().next().getClass())
        .isEqualTo(AzureMonitorMeterRegistry.class);
  }

  @Test
  void shouldCaptureTimeGauge() {
    // when
    recordAndPublish(
        registry ->
            TimeGauge.builder("test-time-gauge", "", MILLISECONDS, obj -> 11.0).register(registry));

    // then
    AgentTestingMicrometerDelegate.Measurement measurement = getLastMeasurement("test-time-gauge");
    assertThat(measurement.value).isEqualTo(11);
    assertThat(measurement.count).isNull();
    assertThat(measurement.min).isNull();
    assertThat(measurement.max).isNull();
    assertThat(measurement.namespace).isNull();
  }

  @Test
  void shouldCaptureGauge() {
    // when
    recordAndPublish(registry -> Gauge.builder("test-gauge", () -> 22.0).register(registry));

    // then
    AgentTestingMicrometerDelegate.Measurement measurement = getLastMeasurement("test-gauge");
    assertThat(measurement.value).isEqualTo(22);
    assertThat(measurement.count).isNull();
    assertThat(measurement.min).isNull();
    assertThat(measurement.max).isNull();
    assertThat(measurement.namespace).isNull();
  }

  @Disabled
  @Test
  void shouldCaptureCounter() {
    // when
    recordAndPublish(
        registry -> {
          Counter counter = Counter.builder("test-counter").register(registry);
          counter.increment(3.3);
        });

    // then
    AgentTestingMicrometerDelegate.Measurement measurement = getLastMeasurement("test-counter");
    assertThat(measurement.value).isEqualTo(3.3);
    assertThat(measurement.count).isNull();
    assertThat(measurement.min).isNull();
    assertThat(measurement.max).isNull();
    assertThat(measurement.namespace).isNull();
  }

  @Test
  void shouldCaptureTimer() {
    // when
    recordAndPublish(
        registry -> {
          Timer timer = Timer.builder("test-timer").register(registry);
          timer.record(Duration.ofMillis(44));
          timer.record(Duration.ofMillis(55));
        });

    // then
    AgentTestingMicrometerDelegate.Measurement measurement = getLastMeasurement("test-timer");
    assertThat(measurement.value).isEqualTo(99);
    assertThat(measurement.count).isEqualTo(2);
    // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    assertThat(measurement.min).isNull();
    assertThat(measurement.max).isEqualTo(55);
    assertThat(measurement.namespace).isNull();
  }

  @Test
  void shouldCaptureDistributionSummary() {
    // when
    recordAndPublish(
        registry -> {
          DistributionSummary distributionSummary =
              DistributionSummary.builder("test-summary").register(registry);
          distributionSummary.record(4.4);
          distributionSummary.record(5.5);
        });

    // then
    AgentTestingMicrometerDelegate.Measurement measurement = getLastMeasurement("test-summary");
    assertThat(measurement.value).isEqualTo(9.9);
    assertThat(measurement.count).isEqualTo(2);
    // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    assertThat(measurement.min).isNull();
    assertThat(measurement.max).isEqualTo(5.5);
    assertThat(measurement.namespace).isNull();
  }

  @Test
  void shouldCaptureLongTaskTimer() {
    // when
    recordAndPublish(
        registry -> {
          LongTaskTimer timer = LongTaskTimer.builder("test-long-task-timer").register(registry);
          timer.start();
          timer.start();
        });

    // then
    AgentTestingMicrometerDelegate.Measurement activeMeasurement =
        getLastMeasurement("test-long-task-timer_active");
    assertThat(activeMeasurement.value).isEqualTo(2);
    assertThat(activeMeasurement.count).isNull();
    assertThat(activeMeasurement.min).isNull();
    assertThat(activeMeasurement.max).isNull();
    assertThat(activeMeasurement.namespace).isNull();

    AgentTestingMicrometerDelegate.Measurement durationMeasurement =
        getLastMeasurement("test-long-task-timer_duration");
    assertThat(durationMeasurement.value).isGreaterThan(50);
    assertThat(durationMeasurement.count).isNull();
    assertThat(durationMeasurement.min).isNull();
    assertThat(durationMeasurement.max).isNull();
    assertThat(durationMeasurement.namespace).isNull();
  }

  @Test
  void shouldCaptureFunctionCounter() {
    // when
    recordAndPublish(
        registry ->
            FunctionCounter.builder("test-function-counter", "", obj -> 6.6).register(registry));

    // then
    AgentTestingMicrometerDelegate.Measurement measurements =
        getLastMeasurement("test-function-counter");
    assertThat(measurements.value).isEqualTo(6.6);
    assertThat(measurements.count).isNull();
    assertThat(measurements.min).isNull();
    assertThat(measurements.max).isNull();
    assertThat(measurements.namespace).isNull();
  }

  @Test
  void shouldCaptureFunctionTimer() {
    // when
    recordAndPublish(
        registry ->
            FunctionTimer.builder("test-function-timer", "", obj -> 2, obj -> 4.4, MILLISECONDS)
                .register(registry));

    // then
    AgentTestingMicrometerDelegate.Measurement measurement =
        getLastMeasurement("test-function-timer");
    assertThat(measurement.value).isEqualTo(4.4);
    assertThat(measurement.count).isEqualTo(2);
    assertThat(measurement.min).isNull();
    assertThat(measurement.max).isNull();
    assertThat(measurement.namespace).isNull();
  }

  // driven by a mock clock instead of the registry's background publisher: a step value that isn't
  // polled in the very next step is reset to zero and lost forever, which made these tests flaky
  // whenever a publish tick was delayed on a busy machine
  private static void recordAndPublish(Consumer<MeterRegistry> recorder) {
    MockClock clock = new MockClock();
    AzureMonitorMeterRegistry registry = new AzureMonitorMeterRegistry(clock);
    try {
      recorder.accept(registry);
      clock.add(AzureMonitorRegistryConfig.INSTANCE.step());
    } finally {
      // stops the background publisher and performs one final publish
      registry.close();
    }
  }

  private static AgentTestingMicrometerDelegate.Measurement getLastMeasurement(String name) {
    List<AgentTestingMicrometerDelegate.Measurement> measurements =
        delegate.getMeasurements().stream()
            .filter(measurement -> measurement.name.equals(name) && measurement.value != 0)
            .collect(Collectors.toList());
    assertThat(measurements)
        .as(
            "non-zero measurements named \"%s\", out of everything published: %s",
            name, describeAll())
        .isNotEmpty();
    return measurements.get(measurements.size() - 1);
  }

  private static String describeAll() {
    return delegate.getMeasurements().stream()
        .map(measurement -> measurement.name + "=" + measurement.value)
        .collect(Collectors.joining(", ", "[", "]"));
  }
}
