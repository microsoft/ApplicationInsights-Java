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

package com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricTelemetryBuilder;

public final class RequestExtractor extends BaseExtractor {

  public static final String REQUESTS_DURATION = "requests/duration";
  public static final String REQUEST_RESULT_CODE = "request/resultCode";
  public static final String REQUEST_SUCCESS = "request/success";

  private final Long statusCode;
  private final boolean success;

  public RequestExtractor(
      MetricTelemetryBuilder telemetryBuilder,
      Long statusCode,
      boolean success,
      boolean isSynthetic) {
    super(telemetryBuilder, isSynthetic);
    this.statusCode = statusCode;
    this.success = success;
  }

  @Override
  public void extract() {
    telemetryBuilder.addProperty(MS_METRIC_ID, REQUESTS_DURATION);
    if (statusCode != null) {
      telemetryBuilder.addProperty(REQUEST_RESULT_CODE, String.valueOf(statusCode));
    }
    telemetryBuilder.addProperty(REQUEST_SUCCESS, success ? TRUE : FALSE);
  }
}
