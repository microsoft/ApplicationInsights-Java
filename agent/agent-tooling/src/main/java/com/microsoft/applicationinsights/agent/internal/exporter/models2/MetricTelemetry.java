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

package com.microsoft.applicationinsights.agent.internal.exporter.models2;

import com.microsoft.applicationinsights.agent.internal.exporter.models.DataPointType;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricDataPoint;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricsData;
import com.microsoft.applicationinsights.agent.internal.telemetry.FormattedTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MetricTelemetry extends Telemetry {

  private final MetricsData data;

  public static MetricTelemetry create() {
    return new MetricTelemetry(new MetricsData());
  }

  public static MetricTelemetry create(String name, double value) {
    MetricTelemetry telemetry = new MetricTelemetry(new MetricsData());

    MetricPointTelemetry point = new MetricPointTelemetry();

    point.setName(name);
    point.setValue(value);
    point.setDataPointType(DataPointType.MEASUREMENT);
    telemetry.setMetricPoint(point);

    telemetry.setTime(FormattedTime.offSetDateTimeFromNow());

    return telemetry;
  }

  private MetricTelemetry(MetricsData data) {
    super(data, "Metric", "MetricData");
    this.data = data;
  }

  public void setMetricPoint(MetricPointTelemetry point) {
    List<MetricDataPoint> metrics = data.getMetrics();
    if (metrics == null) {
      metrics = new ArrayList<>();
      data.setMetrics(metrics);
    }
    if (metrics.isEmpty()) {
      metrics.add(point.getData());
    }
  }

  @Override
  protected Map<String, String> getProperties() {
    Map<String, String> properties = data.getProperties();
    if (properties == null) {
      properties = new HashMap<>();
      data.setProperties(properties);
    }
    return properties;
  }
}
