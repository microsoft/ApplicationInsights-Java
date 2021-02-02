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
import io.opentelemetry.instrumentation.test.AgentTestRunner
import io.opentelemetry.javaagent.instrumentation.micrometer.AzureMonitorMeterRegistry
import io.opentelemetry.javaagent.testing.common.AgentTestingMicrometerDelegateAccess
import java.util.concurrent.Executors

class MicrometerTest extends AgentTestRunner {

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
    def registry = new AzureMonitorMeterRegistry(Clock.SYSTEM)
    TimeGauge.builder("test", "", MILLISECONDS, { 11d }).register(registry)

    when:
    registry.publish()

    then:
    def measurements = AgentTestingMicrometerDelegateAccess.getMeasurements()
    measurements.size() == 1
    measurements[0].name == "test"
    measurements[0].value == 11
    measurements[0].count == null
    measurements[0].min == null
    measurements[0].max == null
  }

  def "should capture gauge"() {
    setup:
    def registry = new AzureMonitorMeterRegistry(Clock.SYSTEM)
    Gauge.builder("test", { 22 }).register(registry)

    when:
    registry.publish()

    then:
    def measurements = AgentTestingMicrometerDelegateAccess.getMeasurements()
    measurements.size() == 1
    measurements[0].name == "test"
    measurements[0].value == 22
    measurements[0].count == null
    measurements[0].min == null
    measurements[0].max == null
  }

  def "should capture counter"() {
    setup:
    def clock = new TestClock()
    def registry = new AzureMonitorMeterRegistry(clock)
    def counter = Counter.builder("test").register(registry)

    when:
    counter.increment(3.3)
    clock.timeMillis = 60000
    registry.publish()

    then:
    def measurements = AgentTestingMicrometerDelegateAccess.getMeasurements()
    measurements.size() == 1
    measurements[0].name == "test"
    measurements[0].value == 3.3
    measurements[0].count == null
    measurements[0].min == null
    measurements[0].max == null
  }

  def "should capture timer"() {
    setup:
    def clock = new TestClock()
    def registry = new AzureMonitorMeterRegistry(clock)
    def timer = Timer.builder("test").register(registry)

    when:
    timer.record(44, MILLISECONDS)
    timer.record(55, MILLISECONDS)
    clock.timeMillis = 60000
    registry.publish()

    then:
    def measurements = AgentTestingMicrometerDelegateAccess.getMeasurements()
    measurements.size() == 1
    measurements[0].name == "test"
    measurements[0].value == 99
    measurements[0].count == 2
    measurements[0].min == null // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    measurements[0].max == 55
  }

  def "should capture distribution summary"() {
    setup:
    def clock = new TestClock()
    def registry = new AzureMonitorMeterRegistry(clock)
    def distributionSummary = DistributionSummary.builder("test").register(registry)

    when:
    distributionSummary.record(4.4)
    distributionSummary.record(5.5)
    clock.timeMillis = 60000
    registry.publish()

    then:
    def measurements = AgentTestingMicrometerDelegateAccess.getMeasurements()
    measurements.size() == 1
    measurements[0].name == "test"
    measurements[0].value == 9.9
    measurements[0].count == 2
    measurements[0].min == null // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    measurements[0].max == 5.5
  }

  def "should capture long task timer"() {
    setup:
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
    def measurements = AgentTestingMicrometerDelegateAccess.getMeasurements()
    measurements.size() == 2
    measurements[0].name == "test_active"
    measurements[0].value == 2
    measurements[0].count == null
    measurements[0].min == null
    measurements[0].max == null
    measurements[1].name == "test_duration"
    measurements[1].value > 150
    measurements[1].count == null
    measurements[1].min == null
    measurements[1].max == null
  }

  def "should capture function counter"() {
    setup:
    def clock = new TestClock()
    def registry = new AzureMonitorMeterRegistry(clock)
    FunctionCounter.builder("test", "", { 6.6d }).register(registry)

    when:
    clock.timeMillis = 60000
    registry.publish()

    then:
    def measurements = AgentTestingMicrometerDelegateAccess.getMeasurements()
    measurements.size() == 1
    measurements[0].name == "test"
    measurements[0].value == 6.6
    measurements[0].count == null
    measurements[0].min == null
    measurements[0].max == null
  }

  def "should capture function timer"() {
    setup:
    def clock = new TestClock()
    def registry = new AzureMonitorMeterRegistry(clock)
    FunctionTimer.builder("test", "", { 2L }, { 4.4d }, MILLISECONDS)
      .register(registry)
    when:
    clock.timeMillis = 60000
    registry.publish()

    then:
    def measurements = AgentTestingMicrometerDelegateAccess.getMeasurements()
    measurements.size() == 1
    measurements[0].name == "test"
    measurements[0].value == 4.4
    measurements[0].count == 2
    measurements[0].min == null
    measurements[0].max == null
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
}
