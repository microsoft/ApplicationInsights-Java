/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import io.opentelemetry.instrumentation.api.aisdk.AiLazyConfiguration;

public class AzureFunctionsInstrumentationHelper {

  public static void lazilyLoadConfiguration() {
    AiLazyConfiguration.lazyLoad();
  }
}
