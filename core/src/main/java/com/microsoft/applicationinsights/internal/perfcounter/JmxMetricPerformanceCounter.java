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

package com.microsoft.applicationinsights.internal.perfcounter;

import java.util.Collection;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.jmx.JmxAttributeData;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

/**
 * A performance counter that sends {@link com.microsoft.applicationinsights.telemetry.MetricTelemetry}
 *
 * Created by gupele on 3/15/2015.
 */
public final class JmxMetricPerformanceCounter extends AbstractJmxPerformanceCounter {

    public JmxMetricPerformanceCounter(String id, String objectName, Collection<JmxAttributeData> attributes) {
        super(id, objectName, attributes);
    }

    @Override
    protected void send(TelemetryClient telemetryClient, String displayName, double value) {
        InternalLogger.INSTANCE.trace("Metric JMX: %s, %s", displayName, value);
	
	MetricTelemetry telemetry = new MetricTelemetry();
	telemetry.markAsCustomPerfCounter();
        telemetry.setName(displayName);
        telemetry.setValue(value);
        telemetry.getProperties().put("CustomPerfCounter", "true");
        telemetryClient.track(telemetry);
    }
}
