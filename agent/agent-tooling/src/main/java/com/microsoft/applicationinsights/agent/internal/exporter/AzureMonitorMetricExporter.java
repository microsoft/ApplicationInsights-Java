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

package com.microsoft.applicationinsights.agent.internal.exporter;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import javax.annotation.Nullable;
import java.util.Collection;

public class AzureMonitorMetricExporter implements MetricExporter {

  private final TelemetryClient telemetryClient;

  public AzureMonitorMetricExporter(TelemetryClient telemetryClient) {
    this.telemetryClient = telemetryClient;
  }

  @Nullable
  @Override
  public AggregationTemporality getPreferredTemporality() {
    return MetricExporter.super.getPreferredTemporality();
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    return null;
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public void close() {
    MetricExporter.super.close();
  }
}
