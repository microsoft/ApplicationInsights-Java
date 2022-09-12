// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import static com.microsoft.applicationinsights.agent.bootstrap.MicrometerUtil.trackMetric;

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
  public AzureMonitorMeterRegistry(Clock clock) {
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
    for (Meter meter : getMeters()) {
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

  private void trackTimeGauge(TimeGauge gauge) {
    trackMetric(
        getName(gauge), gauge.value(getBaseTimeUnit()), null, null, null, getProperties(gauge));
  }

  private void trackGauge(Gauge gauge) {
    trackMetric(getName(gauge), gauge.value(), null, null, null, getProperties(gauge));
  }

  private void trackCounter(Counter counter) {
    trackMetric(getName(counter), counter.count(), null, null, null, getProperties(counter));
  }

  private void trackTimer(Timer timer) {
    long count = timer.count();
    if (count == 0) {
      // important not to send explicit count of 0 because breeze converts that to 1
      return;
    }
    // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    trackMetric(
        getName(timer),
        timer.totalTime(getBaseTimeUnit()),
        castCountToInt(count),
        null,
        timer.max(getBaseTimeUnit()),
        getProperties(timer));
  }

  private void trackDistributionSummary(DistributionSummary summary) {
    long count = summary.count();
    if (count == 0) {
      // important not to send explicit count of 0 because breeze converts that to 1
      return;
    }
    // min is not supported, see https://github.com/micrometer-metrics/micrometer/issues/457
    trackMetric(
        getName(summary),
        summary.totalAmount(),
        castCountToInt(count),
        null,
        summary.max(),
        getProperties(summary));
  }

  private void trackLongTaskTimer(LongTaskTimer timer) {
    Map<String, String> properties = getProperties(timer);
    trackMetric(getName(timer, "active"), timer.activeTasks(), null, null, null, properties);
    trackMetric(
        getName(timer, "duration"),
        timer.duration(getBaseTimeUnit()),
        null,
        null,
        null,
        properties);
  }

  private void trackFunctionCounter(FunctionCounter counter) {
    trackMetric(getName(counter), counter.count(), null, null, null, getProperties(counter));
  }

  private void trackFunctionTimer(FunctionTimer timer) {
    double count = timer.count();
    if (count == 0) {
      // important not to send explicit count of 0 because breeze converts that to 1
      return;
    }
    trackMetric(
        getName(timer),
        timer.totalTime(getBaseTimeUnit()),
        castCountToInt(count),
        null,
        null,
        getProperties(timer));
  }

  private void trackMeter(Meter meter) {
    Map<String, String> properties = getProperties(meter);
    for (Measurement measurement : meter.measure()) {
      trackMetric(
          getName(meter, measurement.getStatistic().toString().toLowerCase()),
          measurement.getValue(),
          null,
          null,
          null,
          properties);
    }
  }

  private String getName(Meter meter) {
    return getName(meter, null);
  }

  private String getName(Meter meter, @Nullable String suffix) {
    Meter.Id meterId = meter.getId();
    return config()
        .namingConvention()
        .name(
            meterId.getName() + (suffix == null ? "" : "." + suffix),
            meterId.getType(),
            meterId.getBaseUnit());
  }

  private Map<String, String> getProperties(Meter meter) {
    Map<String, String> properties = new HashMap<>();
    for (Tag tag : getConventionTags(meter.getId())) {
      properties.put(tag.getKey(), tag.getValue());
    }
    return properties;
  }

  private static int castCountToInt(long count) {
    return count < Integer.MAX_VALUE ? (int) count : Integer.MAX_VALUE;
  }

  private static int castCountToInt(double count) {
    return count < Integer.MAX_VALUE ? (int) count : Integer.MAX_VALUE;
  }
}
