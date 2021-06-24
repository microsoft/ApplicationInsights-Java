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

package com.microsoft.applicationinsights.agent.internal.wascore.heartbeat;

import com.microsoft.applicationinsights.agent.internal.wascore.TelemetryClient;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * <h1>Interface for HeartBeat Properties</h1>
 *
 * <p>This interface defines an implementation for configuring the properties of Application
 * Insights SDK. It allows users to set and get the configuration properties of HeartBeat module. A
 * user can create or bring his own custom implementation of Heartbeat module if wished provided
 * that he abides to the contracts set by this Interface.
 *
 * <p>Default concrete Implementation {@link HeartBeatProvider}
 */
public interface HeartBeatProviderInterface {

  /** Default interval in seconds to transmit heartbeat pulse. */
  long DEFAULT_HEARTBEAT_INTERVAL = TimeUnit.MINUTES.toSeconds(15);

  /** Minimum interval which can be configured by user to transmit heartbeat pulse. */
  long MINIMUM_HEARTBEAT_INTERVAL = 30;

  /**
   * This method initializes the concrete module.
   *
   * @param telemetryClient TelemetryClient
   */
  void initialize(TelemetryClient telemetryClient);

  /**
   * Adds the heartbeat property to the heartbeat payload.
   *
   * @param propertyName Name of the property to be added in Heartbeat payload
   * @param propertyValue Value of the property to be added in Heartbeat payload
   * @param isHealthy indicates if heartbeat is healthy
   * @return true if property is added successfully
   */
  boolean addHeartBeatProperty(String propertyName, String propertyValue, boolean isHealthy);

  /**
   * Returns if heartbeat is enabled or not.
   *
   * @return true if heartbeat is enabled
   */
  boolean isHeartBeatEnabled();

  /**
   * Enables or disables heartbeat module.
   *
   * @param isEnabled state of the heartbeat (enabled/disabled)
   */
  void setHeartBeatEnabled(boolean isEnabled);

  /** Returns the list of Excluded Heart Beat Providers. */
  List<String> getExcludedHeartBeatPropertyProviders();

  /**
   * Sets the list of excluded heartbeat providers.
   *
   * @param excludedHeartBeatPropertyProviders List of heartbeat providers to be excluded
   */
  void setExcludedHeartBeatPropertyProviders(List<String> excludedHeartBeatPropertyProviders);

  /**
   * Gets the currently set heartbeat interval.
   *
   * @return returns the time interval of heartbeat
   */
  long getHeartBeatInterval();

  /**
   * Sets the time interval of heartbeat in seconds.
   *
   * @param timeUnit Heartbeat interval in seconds
   */
  void setHeartBeatInterval(long timeUnit);

  /**
   * Returns the list of excluded heartbeat properties.
   *
   * @return List of excluded heartbeat properties
   */
  List<String> getExcludedHeartBeatProperties();

  /**
   * Sets the list of properties to be excluded from heartbeat payload.
   *
   * @param excludedHeartBeatProperties List of properties to be excluded
   */
  void setExcludedHeartBeatProperties(List<String> excludedHeartBeatProperties);
}
