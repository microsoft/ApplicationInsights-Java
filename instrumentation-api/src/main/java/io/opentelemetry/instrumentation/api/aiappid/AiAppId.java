/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.api.aiappid;

public class AiAppId {

  // forward propagation of appId
  public static final String TRACESTATE_KEY = "az";
  public static final String SPAN_SOURCE_ATTRIBUTE_NAME = "ai.source.appId";

  // backwards propagation of appId
  public static final String RESPONSE_HEADER_NAME = "Request-Context";
  public static final String SPAN_TARGET_ATTRIBUTE_NAME = "ai.target.appId";

  private static volatile Supplier supplier;

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
