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

package com.microsoft.applicationinsights.internal.processor;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.microsoft.applicationinsights.agent.internal.common.StringUtils;
import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.internal.annotation.BuiltInProcessor;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * The class can filter out PageViewTelemetries that
 * have a duration which is less than a predefined value
 * have URLs that has parts that are not needed, i.e. telemetries that will not be sent, based on configuration
 * have unneeded page names i.e. telemetries that will not be sent, that were predefined in configuration
 *
 * Invalid values would prevent the filter from being used.
 *
 * Created by gupele on 7/26/2016.
 */
@BuiltInProcessor("PageViewTelemetryFilter")
public final class PageViewTelemetryFilter implements TelemetryProcessor {

    private long durationThresholdInMS = 0l;
    private final Set<String> notNeededUrls = new HashSet<String>();
    private final Set<String> notNeededNames = new HashSet<String>();

    public PageViewTelemetryFilter() {
    }

    @Override
    public boolean process(Telemetry telemetry) {
        if (telemetry == null) {
            return true;
        }

        if (!(telemetry instanceof PageViewTelemetry)) {
            return true;
        }

        PageViewTelemetry asPVT = (PageViewTelemetry)telemetry;
        URI uri = asPVT.getUri();
        if (uri == null) {
            return true;
        } else {
            String uriPath = uri.toString();
            for (String notNeededUri : notNeededUrls) {
                if (uriPath.contains(notNeededUri)) {
                    return false;
                }
            }
        }

        if (notNeededNames.contains(asPVT.getName())) {
            return false;
        }

        long pvtDuration = asPVT.getDuration();
        if (durationThresholdInMS <= pvtDuration) {
            return true;
        }

        return false;
    }

    public void setDurationThresholdInMS(String durationThresholdInMS) throws NumberFormatException {
        try {
            this.durationThresholdInMS = Long.valueOf(durationThresholdInMS);
            InternalLogger.INSTANCE.trace(String.format("PageViewTelemetryFilter: successfully set DurationThresholdInMS to %s", durationThresholdInMS));
        } catch (NumberFormatException e) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, String.format("PageViewTelemetryFilter: failed to set DurationThresholdInMS: ", durationThresholdInMS));
            throw e;
        }
    }

    public void setNotNeededNames(String notNeededNames) throws Throwable {
        try {
            List<String> notNeededAsList = Arrays.asList(notNeededNames.split(","));
            for (String notNeeded : notNeededAsList) {
                String ready = notNeeded.trim();
                if (StringUtils.isNullOrEmpty(ready)) {
                    continue;
                }

                this.notNeededNames.add(ready);
            }
            InternalLogger.INSTANCE.trace(String.format("PageViewTelemetryFilter: set NotNeededNames: %s", notNeededNames));
        } catch (Throwable t) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, String.format("PageViewTelemetryFilter: failed to parse NotNeededNames: %s", notNeededNames));
            throw t;
        }
    }

    public void setNotNeededUrls(String notNeededUrls) throws Throwable {
        try {
            List<String> notNeededAsList = Arrays.asList(notNeededUrls.split(","));
            for (String notNeeded : notNeededAsList) {
                String ready = notNeeded.trim();
                if (StringUtils.isNullOrEmpty(ready)) {
                    continue;
                }

                this.notNeededUrls.add(ready);
            }
            InternalLogger.INSTANCE.trace("PageViewTelemetryFilter: set " + notNeededUrls);
        } catch (Throwable t) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "PageViewTelemetryFilter: failed to parse NotNeededUrls: " + notNeededUrls);
            throw t;
        }
    }
}
