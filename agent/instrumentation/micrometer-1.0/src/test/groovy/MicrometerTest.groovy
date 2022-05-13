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

import com.microsoft.applicationinsights.agent.bootstrap.MicrometerUtil
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
import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import io.opentelemetry.javaagent.instrumentation.micrometer.AzureMonitorMeterRegistry

import java.util.concurrent.Executors
import java.util.stream.Collectors

import static java.util.concurrent.TimeUnit.MILLISECONDS

class MicrometerTest extends AgentInstrumentationSpecification {

  static final AgentTestingMicrometerDelegate delegate = new AgentTestingMicrometerDelegate()

  static {
    MicrometerUtil.setDelegate(delegate)
  }

  def setup() {
    delegate.reset()
  }

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
    def registry = Metrics.globalRegistry
    TimeGauge.builder("test-time-gauge", "", MILLISECONDS, { 11d }).register(registry)

    when:
    Thread.sleep(500)

    then:
    def measurement = getLastMeasurement("test-time-gauge")
    measurement.value == 11
    measurement.count == null
    measurement.min == null
    measurement.max == null
  }

  def "should capture gauge"() {
    setup:
    def registry = Metrics.globalRegistry

    when:
    Gauge.builder("test-gauge", { 22 }).register(registry)
    Thread.sleep(500)

    then:
    def measurement = getLastMeasurement("test-gauge")
    measurement.value == 22
    measurement.count == null
    measurement.min == null
    measurement.max == null
  }

  def "should capture counter"() {
    setup:
    def registry = Metrics.globalRegistry

    when:
    def counter = Counter.builder("test-counter").register(registry)
    counter.increment(3.3)
    Thread.sleep(500)

    then:
    def measurement = getLastMeasurement("test-counter")
    measurement.value == 3.3
    measurement.count == null
    measurement.min == null
    measurement.max == null
  }

  def "should capture timer"() {
    setup:
    def registry = Metrics.globalRegistry
    def timer = Timer.builder("test-timer").register(registry)

    when:
    timer.record(44, MILLISECONDS)
    timer.record(55, MILLISECONDS)
    Thread.sleep(500)

    then:
    def measurement = getLastMeasurement("test-timer")
    measurement.value == 99
    measurement.count == 2
    measurement.min == null // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    measurement.max == 55
  }

  def "should capture distribution summary"() {
    setup:
    def registry = Metrics.globalRegistry
    def distributionSummary = DistributionSummary.builder("test-summary").register(registry)

    when:
    distributionSummary.record(4.4)
    distributionSummary.record(5.5)
    Thread.sleep(500)

    then:
    def measurement = getLastMeasurement("test-summary")
    measurement.value == 9.9
    measurement.count == 2
    measurement.min == null // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    measurement.max == 5.5
  }

  def "should capture long task timer"() {
    setup:
    def registry = Metrics.globalRegistry

    when:
    def timer = LongTaskTimer.builder("test-long-task-timer").register(registry)
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
    Thread.sleep(500)

    then:
    def activeMeasurement = getLastMeasurement("test-long-task-timer_active")
    activeMeasurement.value == 2
    activeMeasurement.count == null
    activeMeasurement.min == null
    activeMeasurement.max == null

    def durationMeasurement = getLastMeasurement("test-long-task-timer_duration")
    durationMeasurement.value > 50
    durationMeasurement.count == null
    durationMeasurement.min == null
    durationMeasurement.max == null
  }

  def "should capture function counter"() {
    setup:
    def registry = Metrics.globalRegistry

    when:
    FunctionCounter.builder("test-function-counter", "", { 6.6d }).register(registry)
    Thread.sleep(500)

    then:
    def measurements = getLastMeasurement("test-function-counter")
    measurements.value == 6.6
    measurements.count == null
    measurements.min == null
    measurements.max == null
  }

  def "should capture function timer"() {
    setup:
    def registry = Metrics.globalRegistry

    when:
    FunctionTimer.builder("test-function-timer", "", { 2L }, { 4.4d }, MILLISECONDS)
      .register(registry)
    Thread.sleep(500)

    then:
    def measurement = getLastMeasurement("test-function-timer")
    measurement.value == 4.4
    measurement.count == 2
    measurement.min == null
    measurement.max == null
  }

  AgentTestingMicrometerDelegate.Measurement getLastMeasurement(String name) {
    def measurements = delegate.getMeasurements().stream()
      .filter({ it.name == name && it.value != 0 })
      .collect(Collectors.toList())
    return measurements.get(measurements.size() - 1)
  }
}
