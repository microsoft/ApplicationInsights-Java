// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

/**
 * MBean interface for triggering Application Insights profiles via JMX.
 *
 * <p>Can be invoked via jmxterm, JConsole, or any JMX-compatible tool. The MBean is registered
 * under {@code com.microsoft:type=AI-alert,name=ProfilerControl} when {@code
 * enableProfilerControlMBean} is set to {@code true} in the profiler configuration.
 *
 * @see ProfilerControl
 */
public interface ProfilerControlMBean {

  /**
   * Trigger a manual profile with the default duration (120 seconds).
   *
   * @return a status message indicating whether the profile was started
   */
  String triggerProfile();

  /**
   * Trigger a manual profile with the specified duration.
   *
   * @param durationSeconds the desired recording duration in seconds; must be positive
   * @return a status message indicating whether the profile was started
   */
  String triggerProfile(int durationSeconds);
}
