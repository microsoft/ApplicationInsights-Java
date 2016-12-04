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

import java.util.Set;

/**
 * Telemetry Sampler might be used to reduce the amount of telemetries that are sent by the SDK.
 * Sampling reduces the volume of telemetry without affecting your statistics.
 * It keeps together related data points so that you can navigate between them when diagnosing a problem.
 * In the portal, the total counts are multiplied to compensate for the sampling.
 */
public interface TelemetrySampler {
    Set<Class> getExcludeTypes();

    void setExcludeTypes(String types);

    Set<Class> getIncludeTypes();

    void setIncludeTypes(String types);

    Double getSamplingPercentage();

    void setSamplingPercentage(Double samplingPercentage);

    public boolean isSampledIn(Telemetry telemetry);
}
