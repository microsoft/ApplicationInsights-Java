/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.appid;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TargetAppIdAttributeExtractor<REQUEST, RESPONSE>
    extends AttributesExtractor<REQUEST, RESPONSE> {

  public static final AttributeKey<String> SPAN_TARGET_APP_ID_ATTRIBUTE_KEY =
      AttributeKey.stringKey("applicationinsights.internal.target_app_id");

  private final BiFunction<RESPONSE, String, String> headerFunction;

  public TargetAppIdAttributeExtractor(BiFunction<RESPONSE, String, String> headerFunction) {
    this.headerFunction = headerFunction;
  }

  @Override
  protected void onStart(AttributesBuilder attributes, REQUEST request) {}

  @Override
  protected void onEnd(
      AttributesBuilder attributes,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    if (response == null) {
      return;
    }
    String responseHeader = headerFunction.apply(response, AiAppId.RESPONSE_HEADER_NAME);
    if (responseHeader == null) {
      return;
    }
    int index = responseHeader.indexOf('=');
    if (index == -1) {
      return;
    }
    String targetAppId = responseHeader.substring(index + 1);
    attributes.put(SPAN_TARGET_APP_ID_ATTRIBUTE_KEY, targetAppId);
  }
}
