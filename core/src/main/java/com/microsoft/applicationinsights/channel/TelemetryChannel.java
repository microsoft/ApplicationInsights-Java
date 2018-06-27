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

package com.microsoft.applicationinsights.channel;

import com.microsoft.applicationinsights.telemetry.Telemetry;
import java.util.concurrent.TimeUnit;

/** Represents a communication channel for sending telemetry to Azure Application Insights. */
public interface TelemetryChannel {

  /**
   * Gets value indicating whether this channel is in developer mode.
   *
   * @return The developer mode.
   */
  boolean isDeveloperMode();

  /**
   * Sets value indicating whether this channel is in developer mode.
   *
   * @param value True for applying develoer mode
   */
  void setDeveloperMode(boolean value);

  /**
   * Sends a Telemetry instance through the channel.
   *
   * @param item The Telemetry item to send.
   */
  void send(Telemetry item);

  /**
   * Stops on going work
   *
   * @param timeout Time to try and stop
   * @param timeUnit The units of the 'timeout' parameter
   */
  void stop(long timeout, TimeUnit timeUnit);

  /** Flushes the data that the channel might have internally. */
  void flush();

  /**
   * Sets an optional Sampler that can sample out telemetries
   *
   * @param telemetrySampler - The sampler
   */
  void setSampler(TelemetrySampler telemetrySampler);
}
