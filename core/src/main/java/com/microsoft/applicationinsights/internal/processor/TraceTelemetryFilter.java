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

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.internal.annotation.BuiltInProcessor;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * The class can filter out TraceTelemetries ('LOG' telemetries) that have 'lower' severity level than needed
 * The possible values are:
 * OFF             - Filter out ALL traces
 * TRACE           - No filtering. equals to Trace level
 * INFO            - Filter out TRACE level
 * WARN            - Filter out TRACE and INFO
 * ERROR           - Filter out WARN, INFO, TRACE
 * CRITICAL        - filter out all but CRITICAL
 * <p>
 * Illegal value will prevent from the filter from being used.
 * <p>
 * Created by gupele on 7/26/2016.
 */
@BuiltInProcessor("TraceTelemetryFilter")
public final class TraceTelemetryFilter implements TelemetryProcessor {
    private SeverityLevel fromSeverityLevel = null;

    public TraceTelemetryFilter() {
    }

    @Override
    public boolean process(Telemetry telemetry) {
        if (telemetry == null) {
            return true;
        }

        if (!(telemetry instanceof TraceTelemetry)) {
            return true;
        }

        if (fromSeverityLevel == null) {
            return false;
        }

        TraceTelemetry tt = (TraceTelemetry) telemetry;
        String message = tt.getMessage();
        if (LocalStringsUtils.isNullOrEmpty(message)) {
            return true;
        }
        if (tt.getSeverityLevel() == null) {
            return true;
        }

        if (tt.getSeverityLevel().compareTo(this.fromSeverityLevel) < 0) {
            return false;
        }

        return true;
    }

    public void setFromSeverityLevel(String fromSeverityLevel) throws Throwable {
        try {
            String trimmed = fromSeverityLevel.trim();
            if (trimmed.toUpperCase().equals("OFF")) {
                fromSeverityLevel = null;
            } else {
                final Map<String, SeverityLevel> severityLevels = new HashMap<String, SeverityLevel>();
                severityLevels.put("TRACE", SeverityLevel.Verbose);
                severityLevels.put("INFO", SeverityLevel.Information);
                severityLevels.put("WARN", SeverityLevel.Warning);
                severityLevels.put("ERROR", SeverityLevel.Error);
                severityLevels.put("CRITICAL", SeverityLevel.Critical);
                SeverityLevel sl = severityLevels.get(trimmed.toUpperCase());
                if (sl == null) {
                    throw new IllegalArgumentException(String.format("Unknown option: %s", fromSeverityLevel));
                }
                this.fromSeverityLevel = sl;
            }
            InternalLogger.INSTANCE.trace(String.format("TraceTelemetryFilter: set severity level to %s", this.fromSeverityLevel));
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            try {
                this.fromSeverityLevel = SeverityLevel.Verbose;
                InternalLogger.INSTANCE.error("TraceTelemetryFilter: failed to parse: %s, Exception : %s", fromSeverityLevel,
                        ExceptionUtils.getStackTrace(t));
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            } finally {
                throw t;
            }
        }
    }
}
