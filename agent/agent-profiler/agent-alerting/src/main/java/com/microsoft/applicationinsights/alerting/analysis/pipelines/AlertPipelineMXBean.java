// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.pipelines;

// This class name must end in MXBean (case sensitive)
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface AlertPipelineMXBean {

  // Attributes
  long getCooldownSeconds();

  long getProfilerDurationSeconds();

  float getThreshold();

  double getCurrentAverage();

  boolean getEnabled();

  boolean isOffCooldown();

  String getLastAlertTime();

  // Operations
  // - no operations currently implemented
  // Notifications
  // - no notifications currently implemented
}
