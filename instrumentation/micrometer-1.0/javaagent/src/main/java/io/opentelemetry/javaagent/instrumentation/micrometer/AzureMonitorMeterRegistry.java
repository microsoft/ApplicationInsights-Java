/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.micrometer;

import static io.opentelemetry.instrumentation.api.aisdk.MicrometerUtil.trackMetric;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.lang.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AzureMonitorMeterRegistry extends StepMeterRegistry {

  public static final AzureMonitorMeterRegistry INSTANCE =
      new AzureMonitorMeterRegistry(Clock.SYSTEM);

  // visible for testing
  public AzureMonitorMeterRegistry(final Clock clock) {
    super(new AzureMonitorRegistryConfig(), clock);
    config().namingConvention(new AzureMonitorNamingConvention());
    start(new DaemonThreadFactory("azure-micrometer-publisher"));
  }

  @Override
  protected TimeUnit getBaseTimeUnit() {
    return TimeUnit.MILLISECONDS;
  }

  @Override
  protected void publish() {
    for (final Meter meter : getMeters()) {
      if (meter instanceof TimeGauge) {
        trackTimeGauge((TimeGauge) meter);
      } else if (meter instanceof Gauge) {
        trackGauge((Gauge) meter);
      } else if (meter instanceof Counter) {
        trackCounter((Counter) meter);
      } else if (meter instanceof Timer) {
        trackTimer((Timer) meter);
      } else if (meter instanceof DistributionSummary) {
        trackDistributionSummary((DistributionSummary) meter);
      } else if (meter instanceof LongTaskTimer) {
        trackLongTaskTimer((LongTaskTimer) meter);
      } else if (meter instanceof FunctionCounter) {
        trackFunctionCounter((FunctionCounter) meter);
      } else if (meter instanceof FunctionTimer) {
        trackFunctionTimer((FunctionTimer) meter);
      } else {
        trackMeter(meter);
      }
    }
  }

  private void trackTimeGauge(final TimeGauge gauge) {
    trackMetric(
        getName(gauge), gauge.value(getBaseTimeUnit()), null, null, null, getProperties(gauge));
  }

  private void trackGauge(final Gauge gauge) {
    trackMetric(getName(gauge), gauge.value(), null, null, null, getProperties(gauge));
  }

  private void trackCounter(final Counter counter) {
    trackMetric(getName(counter), counter.count(), null, null, null, getProperties(counter));
  }

  private void trackTimer(final Timer timer) {
    // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    trackMetric(
        getName(timer),
        timer.totalTime(getBaseTimeUnit()),
        castCountToInt(timer.count()),
        null,
        timer.max(getBaseTimeUnit()),
        getProperties(timer));
  }

  private void trackDistributionSummary(final DistributionSummary summary) {
    // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    trackMetric(
        getName(summary),
        summary.totalAmount(),
        castCountToInt(summary.count()),
        null,
        summary.max(),
        getProperties(summary));
  }

  private void trackLongTaskTimer(final LongTaskTimer timer) {
    final Map<String, String> properties = getProperties(timer);
    trackMetric(getName(timer, "active"), timer.activeTasks(), null, null, null, properties);
    trackMetric(
        getName(timer, "duration"),
        timer.duration(getBaseTimeUnit()),
        null,
        null,
        null,
        properties);
  }

  private void trackFunctionCounter(final FunctionCounter counter) {
    trackMetric(getName(counter), counter.count(), null, null, null, getProperties(counter));
  }

  private void trackFunctionTimer(final FunctionTimer timer) {
    trackMetric(
        getName(timer),
        timer.totalTime(getBaseTimeUnit()),
        castCountToInt(timer.count()),
        null,
        null,
        getProperties(timer));
  }

  private void trackMeter(final Meter meter) {
    final Map<String, String> properties = getProperties(meter);
    for (final Measurement measurement : meter.measure()) {
      trackMetric(
          getName(meter, measurement.getStatistic().toString().toLowerCase()),
          measurement.getValue(),
          null,
          null,
          null,
          properties);
    }
  }

  private String getName(final Meter meter) {
    return getName(meter, null);
  }

  private String getName(final Meter meter, @Nullable final String suffix) {
    final Meter.Id meterId = meter.getId();
    return config()
        .namingConvention()
        .name(
            meterId.getName() + (suffix == null ? "" : "." + suffix),
            meterId.getType(),
            meterId.getBaseUnit());
  }

  private Map<String, String> getProperties(final Meter meter) {
    final Map<String, String> properties = new HashMap<>();
    for (final Tag tag : getConventionTags(meter.getId())) {
      properties.put(tag.getKey(), tag.getValue());
    }
    return properties;
  }

  private static int castCountToInt(final long count) {
    return count < Integer.MAX_VALUE ? (int) count : Integer.MAX_VALUE;
  }

  private static int castCountToInt(final double count) {
    return count < Integer.MAX_VALUE ? (int) count : Integer.MAX_VALUE;
  }
}
