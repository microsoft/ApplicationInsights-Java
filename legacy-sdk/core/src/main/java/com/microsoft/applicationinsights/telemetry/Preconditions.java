package com.microsoft.applicationinsights.telemetry;

import javax.annotation.Nullable;

final class Preconditions {

  public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(String.valueOf(errorMessage));
    }
  }

  private Preconditions() {}
}
