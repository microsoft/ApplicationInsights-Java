// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap;

import java.util.function.Supplier;
import javax.annotation.Nullable;

public class AzureFunctions {

  private static volatile Supplier<Boolean> hasConnectionString;
  @Nullable private static volatile Runnable configure;

  public static void setup(Supplier<Boolean> hasConnectionString, Runnable initializer) {
    AzureFunctions.hasConnectionString = hasConnectionString;
    AzureFunctions.configure = initializer;
  }

  public static boolean hasConnectionString() {
    return hasConnectionString.get();
  }

  public static void configureOnce() {
    if (configure != null) {
      if (!hasConnectionString()) {
        configure.run();
      }
      configure = null;
    }
  }

  private AzureFunctions() {}
}
