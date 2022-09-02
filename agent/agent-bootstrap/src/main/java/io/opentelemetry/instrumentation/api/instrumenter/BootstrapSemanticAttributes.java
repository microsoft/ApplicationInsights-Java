// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.instrumentation.api.instrumenter;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;

import io.opentelemetry.api.common.AttributeKey;

public final class BootstrapSemanticAttributes {

  // replaced by ai.preview.connection_string
  @Deprecated
  public static final AttributeKey<String> INSTRUMENTATION_KEY =
      AttributeKey.stringKey("ai.preview.instrumentation_key");

  public static final AttributeKey<String> CONNECTION_STRING =
      AttributeKey.stringKey("ai.preview.connection_string");

  public static final AttributeKey<String> ROLE_NAME =
      AttributeKey.stringKey("ai.preview.service_name");

  public static final AttributeKey<Boolean> IS_SYNTHETIC =
      booleanKey("applicationinsights.internal.is_synthetic");
  public static final AttributeKey<Boolean> IS_PRE_AGGREGATED =
      booleanKey("applicationinsights.internal.is_pre_aggregated");

  private BootstrapSemanticAttributes() {}
}
