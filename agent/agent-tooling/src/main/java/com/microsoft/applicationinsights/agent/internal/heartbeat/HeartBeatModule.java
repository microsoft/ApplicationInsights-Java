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

package com.microsoft.applicationinsights.agent.internal.heartbeat;

import com.microsoft.applicationinsights.agent.internal.wascore.TelemetryClient;

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

  /** Interface object holding concrete implementation of heartbeat provider. */
  private final HeartBeatProvider heartBeatProvider;

  public HeartBeatModule() {
    heartBeatProvider = new HeartBeatProvider();
  }

  /**
   * Returns the heartbeat interval in seconds.
   *
   * @return heartbeat interval in seconds.
   */
  public long getHeartBeatInterval() {
    return this.heartBeatProvider.getHeartBeatInterval();
  }

  /**
   * Sets the heartbeat interval in seconds.
   *
   * @param heartBeatInterval Heartbeat interval to set in seconds.
   */
  public void setHeartBeatInterval(long heartBeatInterval) {
    this.heartBeatProvider.setHeartBeatInterval(heartBeatInterval);
  }

  public void initialize(TelemetryClient telemetryClient) {
    heartBeatProvider.initialize(telemetryClient);
  }
}
