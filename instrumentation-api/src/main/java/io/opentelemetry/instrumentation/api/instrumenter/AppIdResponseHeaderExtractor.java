/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;

public abstract class AppIdResponseHeaderExtractor<REQUEST, RESPONSE>
    extends AttributesExtractor<REQUEST, RESPONSE> {

  @Override
  protected void onStart(AttributesBuilder attributes, REQUEST request) {}

  @Override
  protected void onEnd(AttributesBuilder attributes, REQUEST request, RESPONSE response) {
    if (response != null) {
      String responseHeader = header(response, AiAppId.RESPONSE_HEADER_NAME);
      if (responseHeader != null) {
        attributes.put(AiAppId.SPAN_TARGET_APP_ID_ATTRIBUTE_NAME, getTargetAppId(responseHeader));
      }
    }
  }

  protected abstract String header(RESPONSE response, String name);

  private static String getTargetAppId(String responseHeader) {
    if (responseHeader == null) {
      return null;
    }
    final int index = responseHeader.indexOf('=');
    if (index == -1) {
      return null;
    }
    return responseHeader.substring(index + 1);
  }
}
