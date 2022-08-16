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

public class DependencyExtractor extends BaseExtractor {

  // visible for testing
  public static final String DEPENDENCIES_DURATION = "dependencies/duration";
  public static final String DEPENDENCY_TYPE = "dependency/type";
  public static final String DEPENDENCY_PERFORMANCE_BUCKET = "dependency/performanceBucket";
  public static final String DEPENDENCY_SUCCESS = "dependency/success";
  public static final String DEPENDENCY_TARGET = "dependency/target";
  public static final String DEPENDENCY_RESULT_CODE = "dependency/resultCode";

  private final String performanceBucket;
  private final Long statusCode;
  private final boolean success;
  private final String type;
  private final String target;

  @SuppressWarnings("SystemOut")
  public DependencyExtractor(
      AbstractTelemetryBuilder telemetryBuilder,
      String performanceBucket,
      Long statusCode,
      boolean success,
      String type,
      String target) {
    super(telemetryBuilder);
    this.performanceBucket = performanceBucket;
    this.statusCode = statusCode;
    this.success = success;
    this.type = type;
    this.target = target;
  }

  @SuppressWarnings("SystemOut")
  @Override
  public void extract() {
    telemetryBuilder.addProperty(MS_METRIC_ID, DEPENDENCIES_DURATION);
    telemetryBuilder.addProperty(DEPENDENCY_PERFORMANCE_BUCKET, performanceBucket);
    // TODO OTEL will provide rpc.grpc.status_code & rpc.success, http.success
    if (statusCode != null) {
      telemetryBuilder.addProperty(DEPENDENCY_RESULT_CODE, String.valueOf(statusCode));
    }
    telemetryBuilder.addProperty(DEPENDENCY_SUCCESS, success ? TRUE : FALSE);
    telemetryBuilder.addProperty(DEPENDENCY_TYPE, type);
    telemetryBuilder.addProperty(DEPENDENCY_TARGET, target);
  }
}
