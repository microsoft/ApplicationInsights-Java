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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>HeartBeat Provider Module</h1>
 *
 * <p>This module is the core module which is is configured by default with default settings when
 * ApplicationInsights SDK boots up. It is used to transmit diagnostic heartbeats to Application
 * Insights backend.
 */
public class HeartBeatModule {

  private static final Logger logger = LoggerFactory.getLogger(HeartBeatModule.class);

  /** Interface object holding concrete implementation of heartbeat provider. */
  private final HeartBeatProviderInterface heartBeatProviderInterface;

  private static final Object lock = new Object();

  /** Flag to seek if module is initialized. */
  private static volatile boolean isInitialized = false;

  public HeartBeatModule() {
    heartBeatProviderInterface = new HeartBeatProvider();
  }

  /**
   * Returns the heartbeat interval in seconds.
   *
   * @return heartbeat interval in seconds.
   */
  public long getHeartBeatInterval() {
    return this.heartBeatProviderInterface.getHeartBeatInterval();
  }

  /**
   * Sets the heartbeat interval in seconds.
   *
   * @param heartBeatInterval Heartbeat interval to set in seconds.
   */
  public void setHeartBeatInterval(long heartBeatInterval) {
    this.heartBeatProviderInterface.setHeartBeatInterval(heartBeatInterval);
  }

  /** Returns list of excluded heartbeat properties from payload. */
  public List<String> getExcludedHeartBeatProperties() {
    return heartBeatProviderInterface.getExcludedHeartBeatProperties();
  }

  /**
   * Sets list of excluded heartbeat properties provider.
   *
   * @param excludedHeartBeatPropertiesProvider list of excluded heartbeat properties provider to be
   *     excluded.
   */
  public void setExcludedHeartBeatPropertiesProvider(
      List<String> excludedHeartBeatPropertiesProvider) {
    this.heartBeatProviderInterface.setExcludedHeartBeatPropertyProviders(
        excludedHeartBeatPropertiesProvider);
  }

  /**
   * Gets the current state of heartbeat.
   *
   * @return true if enabled
   */
  public boolean isHeartBeatEnabled() {
    return this.heartBeatProviderInterface.isHeartBeatEnabled();
  }

  public void initialize(TelemetryClient telemetryClient) {
    if (!isInitialized && isHeartBeatEnabled()) {
      synchronized (lock) {
        if (!isInitialized && isHeartBeatEnabled()) {
          this.heartBeatProviderInterface.initialize(telemetryClient);
          logger.debug("heartbeat is enabled");
          isInitialized = true;
        }
      }
    }
  }
}
