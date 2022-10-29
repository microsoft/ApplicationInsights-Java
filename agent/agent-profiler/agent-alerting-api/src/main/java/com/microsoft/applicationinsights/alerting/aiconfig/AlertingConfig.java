// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.aiconfig;

public class AlertingConfig {

  public enum RequestFilterType {
    NAME_REGEX
  }

  public static class RequestFilter {
    public final RequestFilterType type;
    public final String value;

    public RequestFilter(RequestFilterType type, String value) {
      this.type = type;
      this.value = value;
    }
  }

  public static class RequestAggregationConfig {

    // Threshold in ms over which a span will consider to be a breach
    // Used by the breach ratio aggregation
    public final int thresholdMillis;

    // Minimum number of samples that must have been collected in order for the aggregation to
    // produce data. Avoids volatile aggregation output on small sample sizes.
    public final int minimumSamples;

    public RequestAggregationConfig(int thresholdMillis, int minimumSamples) {
      this.thresholdMillis = thresholdMillis;
      this.minimumSamples = minimumSamples;
    }
  }

  public enum RequestAggregationType {
    BREACH_RATIO
  }

  public static class RequestAggregation {
    public final RequestAggregationType type;
    public final long windowSizeMillis; // in ms
    public final RequestAggregationConfig configuration;

    public RequestAggregation(
        RequestAggregationType type,
        long windowSizeMillis,
        RequestAggregationConfig configuration) {
      this.type = type;
      this.windowSizeMillis = windowSizeMillis;
      this.configuration = configuration;
    }
  }

  public enum RequestTriggerThresholdType {
    GREATER_THAN
  }

  public static class RequestTriggerThreshold {
    public final RequestTriggerThresholdType type;

    // Threshold value applied to the output of the aggregation
    // i.e :
    //  - For the BreachRatio aggregation, 0.75 means this will trigger if 75% of sample breach the
    // threshold.
    //  - For a rolling average aggregation 0.75 will mean this will trigger if the average request
    // processing time
    //      breaches 0.75ms
    public final float value;

    public RequestTriggerThreshold(RequestTriggerThresholdType type, float value) {
      this.type = type;
      this.value = value;
    }
  }

  public enum RequestTriggerThrottlingType {
    FIXED_DURATION_COOLDOWN
  }

  public static class RequestTriggerThrottling {
    public final RequestTriggerThrottlingType type;
    public final long value; // in seconds

    public RequestTriggerThrottling(RequestTriggerThrottlingType type, long value) {
      this.type = type;
      this.value = value;
    }
  }

  public enum RequestTriggerType {
    LATENCY
  }

  public static class RequestTrigger {
    public final String name;
    public final RequestTriggerType type;
    public final RequestFilter filter;
    public final RequestAggregation aggregation;
    public final RequestTriggerThreshold threshold;
    public final RequestTriggerThrottling throttling;
    public final long profileDuration;

    public RequestTrigger(
        String name,
        RequestTriggerType type,
        RequestFilter filter,
        RequestAggregation aggregation,
        RequestTriggerThreshold threshold,
        RequestTriggerThrottling throttling,
        long profileDuration) {
      this.name = name;
      this.type = type;
      this.filter = filter;
      this.aggregation = aggregation;
      this.threshold = threshold;
      this.throttling = throttling;
      this.profileDuration = profileDuration;
    }
  }

  private AlertingConfig() {}
}
