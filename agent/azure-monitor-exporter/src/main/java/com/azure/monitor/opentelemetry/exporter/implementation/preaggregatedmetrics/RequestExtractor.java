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

public final class RequestExtractor extends BaseExtractor {

  private static final String REQUEST_METRIC_ID = "requests/duration";
  private static final String PERFORMANCE_BUCKET = "request/performanceBucket";
  private static final String REQUEST_RESULT_CODE = "request/resultCode";
  private static final String REQUEST_SUCCESS = "request/success";

  private final String performanceBucket;
  private final Long statusCode;
  private final boolean success;

  public RequestExtractor(
      AbstractTelemetryBuilder telemetryBuilder,
      String performanceBucket,
      Long statusCode,
      boolean success) {
    super(telemetryBuilder);
    this.performanceBucket = performanceBucket;
    this.statusCode = statusCode;
    this.success = success;
  }

  @Override
  public void extract() {
    telemetryBuilder.addProperty(MS_METRIC_ID, REQUEST_METRIC_ID);
    telemetryBuilder.addProperty(PERFORMANCE_BUCKET, performanceBucket);
    telemetryBuilder.addProperty(OPERATION_SYNTHETIC, FALSE);
    if (statusCode != null) {
      telemetryBuilder.addProperty(REQUEST_RESULT_CODE, String.valueOf(statusCode));
    }
    telemetryBuilder.addProperty(REQUEST_SUCCESS, success ? TRUE : FALSE);
  }
}
