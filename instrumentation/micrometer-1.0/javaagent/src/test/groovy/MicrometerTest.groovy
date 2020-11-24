/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS

import io.micrometer.azuremonitor.AzureMonitorConfig
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.FunctionCounter
import io.micrometer.core.instrument.FunctionTimer
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.LongTaskTimer
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.TimeGauge
import io.micrometer.core.instrument.Timer
import io.opentelemetry.instrumentation.api.aisdk.MicrometerUtil
import io.opentelemetry.instrumentation.test.AgentTestRunner
import io.opentelemetry.javaagent.instrumentation.micrometer.AzureMonitorMeterRegistry
import java.util.concurrent.Executors
import spock.lang.Shared

class MicrometerTest extends AgentTestRunner {

  @Shared
  MetricCapturingDelegate delegate

  def "should register AzureMonitorMeterRegistry"() {
    expect:
    Metrics.globalRegistry.registries.size() == 1
    Metrics.globalRegistry.registries[0].class == AzureMonitorMeterRegistry
  }

  def "should not double register AzureMonitorMeterRegistry"() {
    when:
    Metrics.addRegistry(new io.micrometer.azuremonitor.AzureMonitorMeterRegistry(new AzureMonitorConfig() {
      @Override
      String get(String key) {
        return key == "azuremonitor.instrumentationKey" ? "0000" : null
      }
    }, Clock.SYSTEM))

    then:
    Metrics.globalRegistry.registries.size() == 1
    Metrics.globalRegistry.registries[0].class == AzureMonitorMeterRegistry
  }

  def "should capture time gauge"() {
    setup:
    delegate = new MetricCapturingDelegate()
    MicrometerUtil.setDelegate(delegate)

    def registry = new AzureMonitorMeterRegistry(Clock.SYSTEM)
    TimeGauge.builder("test", "", MILLISECONDS, { 11d }).register(registry)

    when:
    registry.publish()

    then:
    delegate.measurements.size() == 1
    delegate.measurements[0].name == "test"
    delegate.measurements[0].value == 11
    delegate.measurements[0].count == null
    delegate.measurements[0].min == null
    delegate.measurements[0].max == null
  }

  def "should capture gauge"() {
    setup:
    delegate = new MetricCapturingDelegate()
    MicrometerUtil.setDelegate(delegate)

    def registry = new AzureMonitorMeterRegistry(Clock.SYSTEM)
    Gauge.builder("test", { 22 }).register(registry)

    when:
    registry.publish()

    then:
    delegate.measurements.size() == 1
    delegate.measurements[0].name == "test"
    delegate.measurements[0].value == 22
    delegate.measurements[0].count == null
    delegate.measurements[0].min == null
    delegate.measurements[0].max == null
  }

  def "should capture counter"() {
    setup:
    delegate = new MetricCapturingDelegate()
    MicrometerUtil.setDelegate(delegate)

    def clock = new TestClock()
    def registry = new AzureMonitorMeterRegistry(clock)
    def counter = Counter.builder("test").register(registry)

    when:
    counter.increment(3.3)
    clock.timeMillis = 60000
    registry.publish()

    then:
    delegate.measurements.size() == 1
    delegate.measurements[0].name == "test"
    delegate.measurements[0].value == 3.3
    delegate.measurements[0].count == null
    delegate.measurements[0].min == null
    delegate.measurements[0].max == null
  }

  def "should capture timer"() {
    setup:
    delegate = new MetricCapturingDelegate()
    MicrometerUtil.setDelegate(delegate)

    def clock = new TestClock()
    def registry = new AzureMonitorMeterRegistry(clock)
    def timer = Timer.builder("test").register(registry)

    when:
    timer.record(44, MILLISECONDS)
    timer.record(55, MILLISECONDS)
    clock.timeMillis = 60000
    registry.publish()

    then:
    delegate.measurements.size() == 1
    delegate.measurements[0].name == "test"
    delegate.measurements[0].value == 99
    delegate.measurements[0].count == 2
    delegate.measurements[0].min == null // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    delegate.measurements[0].max == 55
  }

  def "should capture distribution summary"() {
    setup:
    delegate = new MetricCapturingDelegate()
    MicrometerUtil.setDelegate(delegate)

    def clock = new TestClock()
    def registry = new AzureMonitorMeterRegistry(clock)
    def distributionSummary = DistributionSummary.builder("test").register(registry)

    when:
    distributionSummary.record(4.4)
    distributionSummary.record(5.5)
    clock.timeMillis = 60000
    registry.publish()

    then:
    delegate.measurements.size() == 1
    delegate.measurements[0].name == "test"
    delegate.measurements[0].value == 9.9
    delegate.measurements[0].count == 2
    delegate.measurements[0].min == null // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    delegate.measurements[0].max == 5.5
  }

  def "should capture long task timer"() {
    setup:
    delegate = new MetricCapturingDelegate()
    MicrometerUtil.setDelegate(delegate)

    def registry = new AzureMonitorMeterRegistry(Clock.SYSTEM)
    def timer = LongTaskTimer.builder("test").register(registry)

    when:
    def executor = Executors.newCachedThreadPool()
    executor.execute({
      timer.record({
        Thread.sleep(Long.MAX_VALUE)
      })
    })
    executor.execute({
      timer.record({
        Thread.sleep(Long.MAX_VALUE)
      })
    })
    Thread.sleep(1000)
    registry.publish()

    then:
    delegate.measurements.size() == 2
    delegate.measurements[0].name == "test_active"
    delegate.measurements[0].value == 2
    delegate.measurements[0].count == null
    delegate.measurements[0].min == null
    delegate.measurements[0].max == null
    delegate.measurements[1].name == "test_duration"
    delegate.measurements[1].value > 150
    delegate.measurements[1].count == null
    delegate.measurements[1].min == null
    delegate.measurements[1].max == null
  }

  def "should capture function counter"() {
    setup:
    delegate = new MetricCapturingDelegate()
    MicrometerUtil.setDelegate(delegate)

    def clock = new TestClock()
    def registry = new AzureMonitorMeterRegistry(clock)
    FunctionCounter.builder("test", "", { 6.6d }).register(registry)

    when:
    clock.timeMillis = 60000
    registry.publish()

    then:
    delegate.measurements.size() == 1
    delegate.measurements[0].name == "test"
    delegate.measurements[0].value == 6.6
    delegate.measurements[0].count == null
    delegate.measurements[0].min == null
    delegate.measurements[0].max == null
  }

  def "should capture function timer"() {
    setup:
    delegate = new MetricCapturingDelegate()
    MicrometerUtil.setDelegate(delegate)

    def clock = new TestClock()
    def registry = new AzureMonitorMeterRegistry(clock)
    FunctionTimer.builder("test", "", { 2L }, { 4.4d }, MILLISECONDS)
      .register(registry)
    when:
    clock.timeMillis = 60000
    registry.publish()

    then:
    delegate.measurements.size() == 1
    delegate.measurements[0].name == "test"
    delegate.measurements[0].value == 4.4
    delegate.measurements[0].count == 2
    delegate.measurements[0].min == null
    delegate.measurements[0].max == null
  }

  static class TestClock implements Clock {

    long timeMillis

    @Override
    long wallTime() {
      return timeMillis
    }

    @Override
    long monotonicTime() {
      return MILLISECONDS.toNanos(timeMillis)
    }
  }

  static class MetricCapturingDelegate implements MicrometerUtil.MicrometerUtilDelegate {

    List<Measurement> measurements = new ArrayList<>()

    @Override
    void trackMetric(String name, double value, Integer count, Double min, Double max, Map<String, String> properties) {
      measurements.add(new Measurement(name, value, count, min, max, properties))
    }
  }

  static class Measurement {
    final String name
    final double value
    final Integer count
    final Double min
    final Double max
    final Map<String, String> properties

    Measurement(final String name,
                final double value,
                final Integer count,
                final Double min,
                final Double max, final Map<String, String> properties) {
      this.name = name
      this.value = value
      this.count = count
      this.min = min
      this.max = max
      this.properties = properties
    }
  }
}
