/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.aisdk;

public class AiWebsiteSiteName {

  private static volatile Accessor accessor;

  public static void setAccessor(Accessor accessor) {
    AiWebsiteSiteName.accessor = accessor;
  }

  public static boolean hasWebsiteSiteName() {
    return accessor != null && accessor.hasValue();
  }

  public static void setWebsiteSiteName(String websiteSiteName) {
    if (accessor != null) {
      accessor.setValue(websiteSiteName);
    }
  }

  public interface Accessor {
    boolean hasValue();

    void setValue(String value);
  }
}
