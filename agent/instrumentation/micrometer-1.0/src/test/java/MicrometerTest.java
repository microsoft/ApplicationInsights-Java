// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.microsoft.applicationinsights.agent.bootstrap.MicrometerUtil;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.opentelemetry.javaagent.instrumentation.micrometer.ai.AzureMonitorMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MicrometerTest {

  private static final AgentTestingMicrometerDelegate delegate =
      new AgentTestingMicrometerDelegate();

  static {
    MicrometerUtil.setDelegate(delegate);
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
    // given
    CompositeMeterRegistry registry = Metrics.globalRegistry;
    TimeGauge.builder("test-time-gauge", "", MILLISECONDS, obj -> 11.0).register(registry);

    // then
    await().until(() -> getLastMeasurement("test-time-gauge") != null);

    AgentTestingMicrometerDelegate.Measurement measurement = getLastMeasurement("test-time-gauge");
    assertThat(measurement.value).isEqualTo(11);
    assertThat(measurement.count).isNull();
    assertThat(measurement.min).isNull();
    assertThat(measurement.max).isNull();
    assertThat(measurement.namespace).isNull();
  }

  @Test
  void shouldCaptureGauge() {
    // given
    CompositeMeterRegistry registry = Metrics.globalRegistry;

    // when
    Gauge.builder("test-gauge", () -> 22.0).register(registry);

    // then
    await().until(() -> getLastMeasurement("test-gauge") != null);

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
    // given
    CompositeMeterRegistry registry = Metrics.globalRegistry;

    // when
    Counter counter = Counter.builder("test-counter").register(registry);
    counter.increment(3.3);

    // then
    await().until(() -> getLastMeasurement("test-counter") != null);

    AgentTestingMicrometerDelegate.Measurement measurement = getLastMeasurement("test-counter");
    assertThat(measurement.value).isEqualTo(3.3);
    assertThat(measurement.count).isNull();
    assertThat(measurement.min).isNull();
    assertThat(measurement.max).isNull();
    assertThat(measurement.namespace).isNull();
  }

  @Test
  void shouldCaptureTimer() {
    // given
    CompositeMeterRegistry registry = Metrics.globalRegistry;
    Timer timer = Timer.builder("test-timer").register(registry);

    // when
    timer.record(Duration.ofMillis(44));
    timer.record(Duration.ofMillis(55));

    // then
    await().until(() -> getLastMeasurement("test-timer") != null);

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
    // given
    CompositeMeterRegistry registry = Metrics.globalRegistry;
    DistributionSummary distributionSummary =
        DistributionSummary.builder("test-summary").register(registry);

    // when
    distributionSummary.record(4.4);
    distributionSummary.record(5.5);

    // then
    await().until(() -> getLastMeasurement("test-summary") != null);

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
    // given
    CompositeMeterRegistry registry = Metrics.globalRegistry;

    // when
    LongTaskTimer timer = LongTaskTimer.builder("test-long-task-timer").register(registry);
    ExecutorService executor = Executors.newCachedThreadPool();
    executor.execute(
        () -> {
          timer.record(
              () -> {
                try {
                  Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
              });
        });
    executor.execute(
        () -> {
          timer.record(
              () -> {
                try {
                  Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
              });
        });

    // then
    await()
        .untilAsserted(
            () -> {
              AgentTestingMicrometerDelegate.Measurement activeMeasurement =
                  getLastMeasurement("test-long-task-timer_active");
              assertThat(activeMeasurement).isNotNull();
              assertThat(activeMeasurement.value).isEqualTo(2);
            });

    AgentTestingMicrometerDelegate.Measurement activeMeasurement =
        getLastMeasurement("test-long-task-timer_active");
    assertThat(activeMeasurement.value).isEqualTo(2);
    assertThat(activeMeasurement.count).isNull();
    assertThat(activeMeasurement.min).isNull();
    assertThat(activeMeasurement.max).isNull();
    assertThat(activeMeasurement.namespace).isNull();

    await()
        .untilAsserted(
            () -> {
              AgentTestingMicrometerDelegate.Measurement durationMeasurement =
                  getLastMeasurement("test-long-task-timer_duration");
              assertThat(durationMeasurement).isNotNull();
              assertThat(durationMeasurement.value).isGreaterThan(50);
            });

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
    // given
    CompositeMeterRegistry registry = Metrics.globalRegistry;

    // when
    FunctionCounter.builder("test-function-counter", "", obj -> 6.6).register(registry);

    // then
    await().until(() -> getLastMeasurement("test-function-counter") != null);

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
    // given
    CompositeMeterRegistry registry = Metrics.globalRegistry;

    // when
    FunctionTimer.builder("test-function-timer", "", obj -> 2, obj -> 4.4, MILLISECONDS)
        .register(registry);

    // then
    await().until(() -> getLastMeasurement("test-function-timer") != null);

    AgentTestingMicrometerDelegate.Measurement measurement =
        getLastMeasurement("test-function-timer");
    assertThat(measurement.value).isEqualTo(4.4);
    assertThat(measurement.count).isEqualTo(2);
    assertThat(measurement.min).isNull();
    assertThat(measurement.max).isNull();
    assertThat(measurement.namespace).isNull();
  }

  private static AgentTestingMicrometerDelegate.Measurement getLastMeasurement(String name) {
    List<AgentTestingMicrometerDelegate.Measurement> measurements =
        delegate.getMeasurements().stream()
            .filter(measurement -> measurement.name.equals(name) && measurement.value != 0)
            .collect(Collectors.toList());
    if (measurements.isEmpty()) {
      return null;
    }
    return measurements.get(measurements.size() - 1);
  }
}
