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
import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * The class will filter out telemetries that come from 'unneeded' configured list of source names
 *
 *  Illegal value will prevent from the filter from being used.
 *
 * Created by gupele on 7/26/2016.
 */
@BuiltInProcessor("SyntheticSourceFilter")
public final class SyntheticSourceFilter implements TelemetryProcessor {

    private final Set<String> notNeededSources = new HashSet<String>();

    public SyntheticSourceFilter() {
    }

    @Override
    public boolean process(Telemetry telemetry) {
        if (telemetry == null) {
            return true;
        }

        if (notNeededSources.isEmpty()) {
            return LocalStringsUtils.isNullOrEmpty(telemetry.getContext().getOperation().getSyntheticSource());
        }
        if (notNeededSources.contains(telemetry.getContext().getOperation().getSyntheticSource())) {
            return false;
        }

        return true;
    }

    public void setNotNeededSources(String notNeededSources) throws Throwable {
        try {
            List<String> notNeededAsList = Arrays.asList(notNeededSources.split(","));
            for (String notNeeded : notNeededAsList) {
                String ready = notNeeded.trim();
                if (LocalStringsUtils.isNullOrEmpty(ready)) {
                    continue;
                }

                this.notNeededSources.add(ready);
            }
            InternalLogger.INSTANCE.trace(String.format("SyntheticSourceFilter: set NotNeededSources: %s", notNeededSources));
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable e) {
            try {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, String.format("SyntheticSourceFilter: failed to parse NotNeededSources: %s", notNeededSources));
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            } finally {
                throw e;
            }
        }
    }
}
