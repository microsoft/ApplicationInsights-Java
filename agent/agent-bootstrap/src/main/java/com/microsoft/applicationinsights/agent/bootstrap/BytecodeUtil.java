// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nullable;

// supporting all properties of event, metric, remote dependency and page view telemetry
@SuppressWarnings("TooManyParameters")
public class BytecodeUtil {

  private static BytecodeUtilDelegate delegate;

  public static void setDelegate(BytecodeUtilDelegate delegate) {
    if (BytecodeUtil.delegate == null) {
      BytecodeUtil.delegate = delegate;
      MicrometerUtil.setDelegate(
          new MicrometerUtil.MicrometerUtilDelegate() {
            @Override
            public void trackMetric(
                String name,
                double value,
                Integer count,
                Double min,
                Double max,
                Map<String, String> properties) {
              delegate.trackMetric(
                  null,
                  name,
                  null,
                  value,
                  count,
                  min,
                  max,
                  null,
                  properties,
                  Collections.emptyMap(),
                  null,
                  null);
            }
          });
    }
  }

  public static void setConnectionString(String connectionString) {
    if (delegate != null) {
      delegate.setConnectionString(connectionString);
    }
  }

  public static void trackEvent(
      @Nullable Date timestamp,
      String name,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> metrics,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (delegate != null) {
      delegate.trackEvent(
          timestamp, name, properties, tags, metrics, connectionString, instrumentationKey);
    }
  }

  public static void trackMetric(
      @Nullable Date timestamp,
      String name,
      @Nullable String namespace,
      double value,
      Integer count,
      Double min,
      Double max,
      @Nullable Double stdDev,
      Map<String, String> properties,
      Map<String, String> tags,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (delegate != null) {
      delegate.trackMetric(
          timestamp,
          name,
          namespace,
          value,
          count,
          min,
          max,
          stdDev,
          properties,
          tags,
          connectionString,
          instrumentationKey);
    }
  }

  public static void trackDependency(
      @Nullable Date timestamp,
      String name,
      String id,
      String resultCode,
      Long duration,
      boolean success,
      String commandName,
      String type,
      String target,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> metrics,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (delegate != null) {
      delegate.trackDependency(
          timestamp,
          name,
          id,
          resultCode,
          duration,
          success,
          commandName,
          type,
          target,
          properties,
          tags,
          metrics,
          connectionString,
          instrumentationKey);
    }
  }

  public static void trackPageView(
      @Nullable Date timestamp,
      String name,
      URI uri,
      long totalMillis,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> metrics,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (delegate != null) {
      delegate.trackPageView(
          timestamp,
          name,
          uri,
          totalMillis,
          properties,
          tags,
          metrics,
          connectionString,
          instrumentationKey);
    }
  }

  public static void trackTrace(
      @Nullable Date timestamp,
      String message,
      int severityLevel,
      Map<String, String> properties,
      Map<String, String> tags,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (delegate != null) {
      delegate.trackTrace(
          timestamp,
          message,
          severityLevel,
          properties,
          tags,
          connectionString,
          instrumentationKey);
    }
  }

  public static void trackRequest(
      String id,
      String name,
      URL url,
      @Nullable Date timestamp,
      Long duration,
      String responseCode,
      boolean success,
      String source,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> metrics,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (delegate != null) {
      delegate.trackRequest(
          id,
          name,
          url,
          timestamp,
          duration,
          responseCode,
          success,
          source,
          properties,
          tags,
          metrics,
          connectionString,
          instrumentationKey);
    }
  }

  public static void trackException(
      @Nullable Date timestamp,
      Throwable throwable,
      int severityLevel,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> metrics,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (delegate != null) {
      delegate.trackException(
          timestamp,
          throwable,
          severityLevel,
          properties,
          tags,
          metrics,
          connectionString,
          instrumentationKey);
    }
  }

  public static void trackAvailability(
      @Nullable Date timestamp,
      String id,
      String name,
      @Nullable Long duration,
      boolean success,
      String runLocation,
      String message,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> metrics,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (delegate != null) {
      delegate.trackAvailability(
          timestamp,
          id,
          name,
          duration,
          success,
          runLocation,
          message,
          properties,
          tags,
          metrics,
          connectionString,
          instrumentationKey);
    }
  }

  public static void flush() {
    if (delegate != null) {
      delegate.flush();
    }
  }

  public static void logErrorOnce(Throwable t) {
    if (delegate != null) {
      delegate.logErrorOnce(t);
    }
  }

  public static boolean shouldSample(String operationId) {
    return delegate != null && delegate.shouldSample(operationId);
  }

  public static long getTotalMilliseconds(
      long days, int hours, int minutes, int seconds, int milliseconds) {
    return DAYS.toMillis(days)
        + HOURS.toMillis(hours)
        + MINUTES.toMillis(minutes)
        + SECONDS.toMillis(seconds)
        + milliseconds;
  }

  // originally from SDK MapUtil.copy()
  public static void copy(
      @Nullable Map<String, String> source,
      Map<String, String> target,
      @Nullable String excludePrefix) {
    if (source == null) {
      return;
    }
    for (Map.Entry<String, String> entry : source.entrySet()) {
      String key = entry.getKey();
      if (key == null || key.isEmpty()) {
        continue;
      }
      if (excludePrefix != null && key.startsWith(excludePrefix)) {
        continue;
      }
      if (!target.containsKey(key)) {
        String value = entry.getValue();
        if (value != null) {
          target.put(key, value);
        }
      }
    }
  }

  // this exists only to support -Dapplicationinsights.debug.signedJarAccess=true
  private static final ThreadLocal<Long> startNanosHolder = new ThreadLocal<>();

  public static void onEnter() {
    startNanosHolder.set(System.nanoTime());
  }

  @SuppressWarnings("SystemOut")
  public static void onExit() {
    Long startNanos = startNanosHolder.get();
    if (startNanos == null) {
      System.out.println("Signed jar access (no timing available)");
      Thread.dumpStack();
    } else {
      long durationNanos = System.nanoTime() - startNanos;
      if (durationNanos > MILLISECONDS.toNanos(1)) {
        System.out.println(
            "Signed jar access (" + NANOSECONDS.toMillis(durationNanos) + " milliseconds)");
        Thread.dumpStack();
      }
    }
    startNanosHolder.remove();
  }

  private BytecodeUtil() {}

  public interface BytecodeUtilDelegate {

    void setConnectionString(String connectionString);

    void trackEvent(
        @Nullable Date timestamp,
        String name,
        Map<String, String> properties,
        Map<String, String> tags,
        Map<String, Double> metrics,
        @Nullable String connectionString,
        @Nullable String instrumentationKey);

    void trackMetric(
        @Nullable Date timestamp,
        String name,
        @Nullable String namespace,
        double value,
        Integer count,
        Double min,
        Double max,
        @Nullable Double stdDev,
        Map<String, String> properties,
        Map<String, String> tags,
        @Nullable String connectionString,
        @Nullable String instrumentationKey);

    void trackDependency(
        @Nullable Date timestamp,
        String name,
        String id,
        String resultCode,
        Long duration,
        boolean success,
        String commandName,
        String type,
        String target,
        Map<String, String> properties,
        Map<String, String> tags,
        Map<String, Double> metrics,
        @Nullable String connectionString,
        @Nullable String instrumentationKey);

    void trackPageView(
        @Nullable Date timestamp,
        String name,
        URI uri,
        long totalMillis,
        Map<String, String> properties,
        Map<String, String> tags,
        Map<String, Double> metrics,
        @Nullable String connectionString,
        @Nullable String instrumentationKey);

    void trackTrace(
        @Nullable Date timestamp,
        String message,
        int severityLevel,
        Map<String, String> properties,
        Map<String, String> tags,
        @Nullable String connectionString,
        @Nullable String instrumentationKey);

    void trackRequest(
        String id,
        String name,
        URL url,
        @Nullable Date timestamp,
        Long duration,
        String responseCode,
        boolean success,
        String source,
        Map<String, String> properties,
        Map<String, String> tags,
        Map<String, Double> metrics,
        @Nullable String connectionString,
        @Nullable String instrumentationKey);

    void trackException(
        @Nullable Date timestamp,
        Throwable throwable,
        int severityLevel,
        Map<String, String> properties,
        Map<String, String> tags,
        Map<String, Double> metrics,
        @Nullable String connectionString,
        @Nullable String instrumentationKey);

    void trackAvailability(
        @Nullable Date timestamp,
        String id,
        String name,
        @Nullable Long duration,
        boolean success,
        String runLocation,
        String message,
        Map<String, String> properties,
        Map<String, String> tags,
        Map<String, Double> metrics,
        @Nullable String connectionString,
        @Nullable String instrumentationKey);

    void flush();

    void logErrorOnce(Throwable t);

    boolean shouldSample(String shouldSample);
  }
}
