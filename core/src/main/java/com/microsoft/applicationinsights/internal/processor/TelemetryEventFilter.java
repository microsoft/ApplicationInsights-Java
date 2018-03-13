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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.internal.annotation.BuiltInProcessor;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * The class will filter out Event Telemetries with unneeded values
 *
 *  Illegal value will prevent from the filter from being used.
 *
 * Created by gupele on 7/26/2016.
 */
@BuiltInProcessor("TelemetryEventFilter")
public final class TelemetryEventFilter implements TelemetryProcessor {
    private final Set<String> notNeededNames = new HashSet<String>();

    public TelemetryEventFilter() {
    }

    @Override
    public boolean process(Telemetry telemetry) {
        if (telemetry == null) {
            return true;
        }

        if (!(telemetry instanceof EventTelemetry)) {
            return true;
        }

        EventTelemetry et = (EventTelemetry)telemetry;
        String eventName = et.getName();
        if (LocalStringsUtils.isNullOrEmpty(eventName)) {
            return true;
        }

        return !notNeededNames.contains(eventName);
    }

    public void setNotNeededNames(String notNeededNames) throws Throwable {
        try {
            List<String> notNeededAsList = Arrays.asList(notNeededNames.split(","));
            for (String notNeeded : notNeededAsList) {
                String ready = notNeeded.trim();
                if (LocalStringsUtils.isNullOrEmpty(ready)) {
                    continue;
                }

                this.notNeededNames.add(ready);
            }
            InternalLogger.INSTANCE.trace(String.format("TelemetryEventFilter: set NotNeededNames: %s", notNeededNames));
        } catch (Throwable e) {
            InternalLogger.INSTANCE.error("TelemetryEventFilter: failed to parse NotNeededNames: %s Exception : %s", notNeededNames,
                    ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }
}
