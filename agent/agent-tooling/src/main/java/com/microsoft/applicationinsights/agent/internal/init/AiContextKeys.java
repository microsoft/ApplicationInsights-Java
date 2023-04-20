// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import io.opentelemetry.context.ContextKey;

public final class AiContextKeys {

  public static final ContextKey<String> CONNECTION_STRING =
      ContextKey.named("applicationinsights.internal.connection_string");

  public static final ContextKey<String> ROLE_NAME =
      ContextKey.named("applicationinsights.internal.role_name");

  private AiContextKeys() {}
}
