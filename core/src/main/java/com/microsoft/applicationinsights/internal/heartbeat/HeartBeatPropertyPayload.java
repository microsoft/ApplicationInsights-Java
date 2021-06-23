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

package com.microsoft.applicationinsights.internal.heartbeat;

/**
 * Defines the Payload class to store and send heartbeat properties and allowing to keep track of
 * updates to them.
 */
public class HeartBeatPropertyPayload {

  /** Value of payload on initialization. Ready for transmission. */
  private String payloadValue = "";

  /** Is this healthy property or not. */
  private boolean isHealthy = false;

  /**
   * Returns the payload value
   *
   * @return String value of payload property
   */
  String getPayloadValue() {
    return payloadValue;
  }

  /**
   * This is used to set the payload
   *
   * @param payloadValue value of the property
   */
  public void setPayloadValue(String payloadValue) {
    if (payloadValue != null && !this.payloadValue.equals(payloadValue)) {
      this.payloadValue = payloadValue;
    }
  }

  /**
   * Gets the value of payload is healthy
   *
   * @return returns true of payload value is healthy.
   */
  public boolean isHealthy() {
    return isHealthy;
  }

  /**
   * Sets the health of the payload.
   *
   * @param healthy boolean value representing the health.
   */
  public void setHealthy(boolean healthy) {
    this.isHealthy = healthy;
  }
}
