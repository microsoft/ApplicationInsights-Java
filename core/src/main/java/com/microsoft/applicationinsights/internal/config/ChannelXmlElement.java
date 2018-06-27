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

package com.microsoft.applicationinsights.internal.config;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/** Created by gupele on 3/15/2015. */
public class ChannelXmlElement {

  private String endpointAddress;
  private String maxTelemetryBufferCapacity;
  private String flushIntervalInSeconds;
  private boolean developerMode;
  private boolean throttling = true;
  private String maxTransmissionStorageFilesCapacityInMB;
  private String maxInstantRetry;
  private String type =
      "com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel";

  public String getType() {
    return type;
  }

  @XmlAttribute(name = "type")
  public void setType(String type) {
    this.type = type;
  }

  public String getEndpointAddress() {
    return endpointAddress;
  }

  @XmlElement(name = "EndpointAddress")
  public void setEndpointAddress(String endpointAddress) {
    this.endpointAddress = endpointAddress;
  }

  public boolean getThrottling() {
    return throttling;
  }

  @XmlElement(name = "Throttling")
  public void setThrottling(boolean throttling) {
    this.throttling = throttling;
  }

  public boolean getDeveloperMode() {
    return developerMode;
  }

  @XmlElement(name = "DeveloperMode")
  public void setDeveloperMode(boolean developerMode) {
    this.developerMode = developerMode;
  }

  public String getMaxTelemetryBufferCapacity() {
    return maxTelemetryBufferCapacity;
  }

  @XmlElement(name = "MaxTelemetryBufferCapacity")
  public void setMaxTelemetryBufferCapacity(String maxTelemetryBufferCapacity) {
    this.maxTelemetryBufferCapacity = maxTelemetryBufferCapacity;
  }

  public String getFlushIntervalInSeconds() {
    return flushIntervalInSeconds;
  }

  @XmlElement(name = "FlushIntervalInSeconds")
  public void setFlushIntervalInSeconds(String flushIntervalInSeconds) {
    this.flushIntervalInSeconds = flushIntervalInSeconds;
  }

  public String isMaxTransmissionStorageFilesCapacityInMB() {
    return maxTransmissionStorageFilesCapacityInMB;
  }

  @XmlElement(name = "MaxTransmissionStorageFilesCapacityInMB")
  public void setMaxTransmissionStorageFilesCapacityInMB(
      String maxTransmissionStorageFilesCapacityInMB) {
    this.maxTransmissionStorageFilesCapacityInMB = maxTransmissionStorageFilesCapacityInMB;
  }

  public String getMaxInstantRetry() {
    return maxInstantRetry;
  }

  @XmlElement(name = "MaxInstantRetry")
  public void setMaxInstantRetry(String maxInstantRetry) {
    this.maxInstantRetry = maxInstantRetry;
  }

  public Map<String, String> getData() {
    HashMap<String, String> data = new HashMap<String, String>();
    if (developerMode) {
      data.put("DeveloperMode", "true");
    }

    if (!Strings.isNullOrEmpty(endpointAddress)) {
      data.put("EndpointAddress", endpointAddress);
    }

    if (!Strings.isNullOrEmpty(maxTelemetryBufferCapacity)) {
      data.put("MaxTelemetryBufferCapacity", maxTelemetryBufferCapacity);
    }

    if (!Strings.isNullOrEmpty(flushIntervalInSeconds)) {
      data.put("FlushIntervalInSeconds", flushIntervalInSeconds);
    }

    if (!Strings.isNullOrEmpty(maxTransmissionStorageFilesCapacityInMB)) {
      data.put("MaxTransmissionStorageFilesCapacityInMB", maxTransmissionStorageFilesCapacityInMB);
    }

    if (!Strings.isNullOrEmpty(maxInstantRetry)) {
      data.put("MaxInstantRetry", maxInstantRetry);
    }

    data.put("Throttling", throttling ? "true" : "false");

    return data;
  }
}
