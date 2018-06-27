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

package com.microsoft.applicationinsights.internal.channel.sampling;

import com.microsoft.applicationinsights.channel.TelemetrySampler;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;
import com.microsoft.applicationinsights.internal.shutdown.Stoppable;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This sampler will keep the outgoing telemetries within the limit that was decided by the user.
 *
 * <p>This sampler will change the sampling rate as needed to keep up in the pace, as opposed to the
 * 'FixedRateTelemetrySampler' This sampler employs the {@link FixedRateTelemetrySampler} for doing
 * the actual sampling and a timer for re-evaluating the sampling percentage.
 *
 * <p>Created by gupele on 11/9/2016.
 */
public final class AdaptiveTelemetrySampler implements Stoppable, TelemetrySampler {
  private static final int DEFAULT_MAX_TELEMETRIES_PER_SECOND = 100;
  private static final int DEFAULT_EVALUATION_INTERVAL_IN_SECONDS = 900;
  private static final int DEFAULT_SAMPLING_PERCENTAGE_DECREASE_TIMEOUT_IN_SECONDS = 120;
  private static final int DEFAULT_SAMPLING_PERCENTAGE_INCREASE_TIMEOUT_IN_SECONDS = 900;
  private static final int DEFAULT_MIN_SAMPLING_PERCENTAGE = 1;
  private static final int DEFAULT_MAX_SAMPLING_PERCENTAGE = 100;
  private static final int DEFAULT_INITIAL_SAMPLING_PERCENTAGE = 100;
  private static final double DEFAULT_MOVING_AVERAGE_RATIO = 0.25;
  private final AtomicLong counter = new AtomicLong(0);;
  // We use the 'FixedRateTelemetrySampler' to do the actual sampling
  private final FixedRateTelemetrySampler sampler = new FixedRateTelemetrySampler();
  // Max telemetries per second, the instance will
  // try to keep up by adjusting the sampling percentage
  private double maxTelemetriesPerSecond;

  // How much time to wait between evaluations of the sampling percentage
  private int evaluationIntervalInSec;

  // How much time to wait between successive increases of the sampling percentage
  private int samplingPercentageDecreaseTimeoutInSec;

  // How much time to wait between successive decreases of the sampling percentage
  private int samplingPercentageIncreaseTimeoutInSec;

  private int minSamplingPercentage;
  private int maxSamplingPercentage;

  // The weigh given to the last telemetries count within 'evaluationIntervalInSeconds'
  private double movingAverageRatio = 0.25;

  private double currentSamplingPercentage;
  private Date lastChangedDate;

  // This is for working with 'samplingPercentageDecreaseTimeoutInSeconds' and ...Increase....
  private ChangeDirection lastChangeDirection = ChangeDirection.None;
  private ScheduledThreadPoolExecutor threads;

  @Override
  public synchronized void stop(long timeout, TimeUnit timeUnit) {
    ThreadPoolUtils.stop(threads, timeout, timeUnit);
  }

  /**
   * This method must be called prior to any use of the instance
   *
   * @param maxTelemetriesPerSecond maxTelemetriesPerSecond
   * @param evaluationIntervalInSeconds evaluationIntervalInSeconds
   * @param samplingPercentageDecreaseTimeoutInSeconds samplingPercentageDecreaseTimeoutInSeconds
   * @param samplingPercentageIncreaseTimeoutInSeconds samplingPercentageIncreaseTimeoutInSeconds
   * @param minSamplingPercentage minSamplingPercentage
   * @param maxSamplingPercentage maxSamplingPercentage
   * @param initialSamplingPercentage initialSamplingPercentage
   * @param movingAverageRatio movingAverageRatio
   */
  public void initialize(
      String maxTelemetriesPerSecond,
      String evaluationIntervalInSeconds,
      String samplingPercentageDecreaseTimeoutInSeconds,
      String samplingPercentageIncreaseTimeoutInSeconds,
      String minSamplingPercentage,
      String maxSamplingPercentage,
      String initialSamplingPercentage,
      String movingAverageRatio) {
    this.maxTelemetriesPerSecond =
        getIntValueOrDefault(
            "maxTelemetriesPerSecond",
            maxTelemetriesPerSecond,
            DEFAULT_MAX_TELEMETRIES_PER_SECOND,
            0,
            Integer.MAX_VALUE);
    this.evaluationIntervalInSec =
        getIntValueOrDefault(
            "evaluationIntervalInSec",
            evaluationIntervalInSeconds,
            DEFAULT_EVALUATION_INTERVAL_IN_SECONDS,
            0,
            Integer.MAX_VALUE);
    this.samplingPercentageDecreaseTimeoutInSec =
        getIntValueOrDefault(
            "samplingPercentageDecreaseTimeoutInSec",
            samplingPercentageDecreaseTimeoutInSeconds,
            DEFAULT_SAMPLING_PERCENTAGE_DECREASE_TIMEOUT_IN_SECONDS,
            0,
            Integer.MAX_VALUE);
    this.samplingPercentageIncreaseTimeoutInSec =
        getIntValueOrDefault(
            "samplingPercentageIncreaseTimeoutInSec",
            samplingPercentageIncreaseTimeoutInSeconds,
            DEFAULT_SAMPLING_PERCENTAGE_INCREASE_TIMEOUT_IN_SECONDS,
            0,
            Integer.MAX_VALUE);
    this.minSamplingPercentage =
        getIntValueOrDefault(
            "minSamplingPercentage",
            minSamplingPercentage,
            DEFAULT_MIN_SAMPLING_PERCENTAGE,
            0,
            100);
    this.maxSamplingPercentage =
        getIntValueOrDefault(
            "maxSamplingPercentage",
            maxSamplingPercentage,
            DEFAULT_MAX_SAMPLING_PERCENTAGE,
            0,
            100);
    this.currentSamplingPercentage =
        getDoubleValueOrDefault(
            "initialSamplingPercentage",
            initialSamplingPercentage,
            DEFAULT_INITIAL_SAMPLING_PERCENTAGE,
            0.0,
            100.0);
    this.movingAverageRatio =
        getDoubleValueOrDefault(
            "movingAverageRatio", movingAverageRatio, DEFAULT_MOVING_AVERAGE_RATIO, 0.0, 100.0);

    createTimerThread();

    lastChangedDate = new Date();
    sampler.setSamplingPercentage(this.currentSamplingPercentage);
    threads.scheduleAtFixedRate(
        new SamplingRangeEvaluator(),
        this.evaluationIntervalInSec,
        this.evaluationIntervalInSec,
        TimeUnit.SECONDS);
    SDKShutdownActivity.INSTANCE.register(this);
  }

  @Override
  public Set<Class> getExcludeTypes() {
    return sampler.getExcludeTypes();
  }

  @Override
  public void setExcludeTypes(String types) {
    sampler.setExcludeTypes(types);
  }

  @Override
  public Set<Class> getIncludeTypes() {
    return sampler.getIncludeTypes();
  }

  @Override
  public void setIncludeTypes(String types) {
    sampler.setIncludeTypes(types);
  }

  @Override
  public Double getSamplingPercentage() {
    return sampler.getSamplingPercentage();
  }

  @Override
  public void setSamplingPercentage(Double samplingPercentage) {
    sampler.setSamplingPercentage(samplingPercentage);
  }

  @Override
  public boolean isSampledIn(Telemetry telemetry) {
    if (sampler.isSampledIn(telemetry)) {
      counter.incrementAndGet();
      return true;
    }

    return false;
  }

  private void createTimerThread() {
    threads = new ScheduledThreadPoolExecutor(1);
    threads.setThreadFactory(
        ThreadPoolUtils.createDaemonThreadFactory(AdaptiveTelemetrySampler.class));
  }

  private int getIntValueOrDefault(
      String name, String valueAsString, int defaultValue, int minValue, int maxValue) {
    int result = defaultValue;
    try {
      int value = Integer.valueOf(valueAsString);
      if (value > 0) {
        result = value;
      }
    } catch (Exception e) {
    }

    if (result > maxValue) {
      result = maxValue;
    }
    if (result < minValue) {
      result = minValue;
    }

    InternalLogger.INSTANCE.trace("%s is set to %s", name, defaultValue);
    return result;
  }

  private double getDoubleValueOrDefault(
      String name, String valueAsString, double defaultValue, double minValue, double maxValue) {
    double result = defaultValue;
    try {
      double value = Double.valueOf(valueAsString);
      if (value > 0) {
        result = value;
      }
    } catch (Exception e) {
    }

    if (result > maxValue) {
      result = maxValue;
    }
    if (result < minValue) {
      result = minValue;
    }

    InternalLogger.INSTANCE.trace("%s is set to %s", name, defaultValue);
    return result;
  }

private enum ChangeDirection {
    Up,
    Down,
    None
  }

  private class SamplingRangeEvaluator implements Runnable {
    private boolean first = true;
    private double average = 0.0;

    @Override
    public void run() {
      double telemetriesPerSecond = (double) counter.get() / (double) evaluationIntervalInSec;
      counter.set(0);
      if (!first) {
        average = average * (1 - movingAverageRatio) + telemetriesPerSecond * movingAverageRatio;
      } else {
        first = false;
        average = telemetriesPerSecond;
      }

      InternalLogger.INSTANCE.trace("Average for sampling is %s", average);

      double suggestedSamplingPercentage;
      if (average > maxTelemetriesPerSecond) {
        suggestedSamplingPercentage =
            100.0 - (average - maxTelemetriesPerSecond) * 100.0 / maxTelemetriesPerSecond;
      } else {
        suggestedSamplingPercentage = 100;
      }
      if (suggestedSamplingPercentage > maxSamplingPercentage) {
        suggestedSamplingPercentage = maxSamplingPercentage;
      }
      if (suggestedSamplingPercentage < minSamplingPercentage) {
        suggestedSamplingPercentage = minSamplingPercentage;
      }

      boolean samplingPercentageChangeNeeded =
          suggestedSamplingPercentage != currentSamplingPercentage;
      if (samplingPercentageChangeNeeded) {
        Date currentDate = new Date();
        long duration = currentDate.getTime() - lastChangedDate.getTime();

        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        if (suggestedSamplingPercentage > currentSamplingPercentage) {
          if (lastChangeDirection != ChangeDirection.Up
              || diffInSeconds >= samplingPercentageIncreaseTimeoutInSec) {
            updateSamplingData(suggestedSamplingPercentage, ChangeDirection.Up, currentDate);
          }
        } else {
          if (lastChangeDirection != ChangeDirection.Down
              || diffInSeconds >= samplingPercentageDecreaseTimeoutInSec) {
            updateSamplingData(suggestedSamplingPercentage, ChangeDirection.Down, currentDate);
          }
        }
      }
    }

    private void updateSamplingData(
        double suggestedSamplingPercentage, ChangeDirection direction, Date currentDate) {
      InternalLogger.INSTANCE.trace(
          "Updating sampling percentage from %s to %s",
          currentSamplingPercentage, suggestedSamplingPercentage);
      currentSamplingPercentage = suggestedSamplingPercentage;
      lastChangeDirection = direction;
      lastChangedDate = currentDate;
      sampler.setSamplingPercentage((double) suggestedSamplingPercentage);
    }
  }
}
