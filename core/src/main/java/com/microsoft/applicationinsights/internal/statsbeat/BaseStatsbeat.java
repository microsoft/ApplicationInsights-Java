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

package com.microsoft.applicationinsights.internal.statsbeat;

import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.FormattedTime;
import com.microsoft.applicationinsights.TelemetryClient;

import java.util.HashMap;
import java.util.Map;

abstract class BaseStatsbeat {

    private static final String STATSBEAT_TELEMETRY_NAME = "Statsbeat";

    private final CustomDimensions customDimensions;

    protected BaseStatsbeat(CustomDimensions customDimensions) {
        this.customDimensions = customDimensions;
    }

    protected abstract void send(TelemetryClient telemetryClient);

    protected TelemetryItem createStatsbeatTelemetry(TelemetryClient telemetryClient, String name, double value) {
        TelemetryItem telemetry = new TelemetryItem();
        MetricsData data = new MetricsData();
        MetricDataPoint point = new MetricDataPoint();
        telemetryClient.initMetricTelemetry(telemetry, data, point);
        // overwrite the default name (which is "Metric")
        telemetry.setName(STATSBEAT_TELEMETRY_NAME);

        point.setName(name);
        point.setValue(value);
        point.setDataPointType(DataPointType.MEASUREMENT);

        telemetry.setInstrumentationKey(telemetryClient.getStatsbeatInstrumentationKey());
        telemetry.setTime(FormattedTime.fromNow());

        Map<String, String> properties = new HashMap<>();
        customDimensions.populateProperties(properties, telemetryClient.getInstrumentationKey());
        data.setProperties(properties);
        return telemetry;
    }
}