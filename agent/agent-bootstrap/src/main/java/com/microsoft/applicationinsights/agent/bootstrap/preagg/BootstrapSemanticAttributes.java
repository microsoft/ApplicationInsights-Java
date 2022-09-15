// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.preagg;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;

import io.opentelemetry.api.common.AttributeKey;

public class BootstrapSemanticAttributes {

  // marks whether a request is coming from a "real" user, or a "synthetic" user (e.g. a bot or
  // health check)
  public static final AttributeKey<Boolean> IS_SYNTHETIC =
      booleanKey("applicationinsights.internal.is_synthetic");

  private BootstrapSemanticAttributes() {}
}
