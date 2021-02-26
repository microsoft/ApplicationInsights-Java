/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.aiconnectionstring;

public class AiConnectionString {

  private static volatile Accessor accessor;

  public static void setAccessor(Accessor accessor) {
    AiConnectionString.accessor = accessor;
  }

  public static boolean hasConnectionString() {
    return accessor != null && accessor.hasValue();
  }

  public static void setConnectionString(String connectionString) {
    if (accessor != null) {
      accessor.setValue(connectionString);
    }
  }

  public interface Accessor {
    boolean hasValue();

    void setValue(String value);
  }
}
