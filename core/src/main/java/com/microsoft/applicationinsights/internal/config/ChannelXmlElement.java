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

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Created by gupele on 3/15/2015.
 */
public class ChannelXmlElement {

    @XStreamAlias("EndpointAddress")
    private String endpointAddress;

    @XStreamAlias("MaxTelemetryBufferCapacity")
    private String maxTelemetryBufferCapacity;

    @XStreamAlias("FlushIntervalInSeconds")
    private String flushIntervalInSeconds;

    @XStreamAlias("DeveloperMode")
    private boolean developerMode;

    @XStreamAlias("Throttling")
    private boolean throttling = true;

    @XStreamAlias("MaxTransmissionStorageFilesCapacityInMB")
    private String maxTransmissionStorageFilesCapacityInMB;

    @XStreamAlias("MaxInstantRetry")
    private String maxInstantRetry;

    @XStreamAsAttribute
    private String type = "com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEndpointAddress() {
        return endpointAddress;
    }

    public void setThrottling(boolean throttling) {
        this.throttling = throttling;
    }

    public boolean getThrottling() {
        return throttling;
    }

    public void setEndpointAddress(String endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    public boolean getDeveloperMode() {
        return developerMode;
    }

    public void setDeveloperMode(boolean developerMode) {
        this.developerMode = developerMode;
    }

    public String getMaxTelemetryBufferCapacity() {
        return maxTelemetryBufferCapacity;
    }

    public void setMaxTelemetryBufferCapacity(String maxTelemetryBufferCapacity) {
        this.maxTelemetryBufferCapacity = maxTelemetryBufferCapacity;
    }

    public String getFlushIntervalInSeconds() {
        return flushIntervalInSeconds;
    }

    public void setFlushIntervalInSeconds(String flushIntervalInSeconds) {
        this.flushIntervalInSeconds = flushIntervalInSeconds;
    }

    public String isMaxTransmissionStorageFilesCapacityInMB() {
        return maxTransmissionStorageFilesCapacityInMB;
    }

    public void setMaxTransmissionStorageFilesCapacityInMB(String maxTransmissionStorageFilesCapacityInMB) {
        this.maxTransmissionStorageFilesCapacityInMB = maxTransmissionStorageFilesCapacityInMB;
    }


    public String getMaxInstantRetry() {
        return maxInstantRetry;
    }

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
