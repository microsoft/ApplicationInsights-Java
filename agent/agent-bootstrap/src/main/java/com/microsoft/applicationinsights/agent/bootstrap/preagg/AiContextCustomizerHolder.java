// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.preagg;

import io.opentelemetry.instrumentation.api.instrumenter.ContextCustomizer;

public final class AiContextCustomizerHolder {

  private static volatile ContextCustomizer<Object> instance;

  public static void setInstance(ContextCustomizer<Object> instance) {
    AiContextCustomizerHolder.instance = instance;
  }

  public static ContextCustomizer<Object> getInstance() {
    return instance;
  }

  private AiContextCustomizerHolder() {}
}
