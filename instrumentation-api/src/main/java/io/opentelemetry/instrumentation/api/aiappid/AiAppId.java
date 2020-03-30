/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.aiappid;

public class AiAppId {

  // forward propagation of appId
  public static final String TRACESTATE_KEY = "az";
  public static final String SPAN_SOURCE_ATTRIBUTE_NAME =
      "applicationinsights.internal.source_app_id";

  // backwards propagation of appId
  public static final String RESPONSE_HEADER_NAME = "Request-Context";
  public static final String SPAN_TARGET_ATTRIBUTE_NAME =
      "applicationinsights.internal.target_app_id";

  private static volatile Supplier supplier;

  static {
    String testingAppId = System.getProperty("ai.internal.testing.appId");
    if (testingAppId != null) {
      supplier = () -> testingAppId;
    }
  }

  public static void setSupplier(final Supplier supplier) {
    AiAppId.supplier = supplier;
  }

  public static String getAppId() {
    return supplier == null ? "" : supplier.get();
  }

  public interface Supplier {
    String get();
  }
}
