/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.aisdk;

public class AiLazyConfiguration {

  private static volatile Accessor accessor;

  public static void setAccessor(Accessor accessor) {
    AiLazyConfiguration.accessor = accessor;
  }

  public static void lazyLoad() {
    if (accessor != null) {
      accessor.lazyLoad();
    }
  }

  public interface Accessor {
    void lazyLoad();
  }
}
