package com.azure.monitor.opentelemetry.exporter.implementation;

import io.opentelemetry.api.common.AttributeKey;

public final class AiSemanticAttributes {

  public static final AttributeKey<String> OPERATION_NAME =
      AttributeKey.stringKey("applicationinsights.internal.operation_name");

  public static final AttributeKey<Long> ITEM_COUNT =
      AttributeKey.longKey("applicationinsights.internal.item_count");

  private AiSemanticAttributes() {}
}
