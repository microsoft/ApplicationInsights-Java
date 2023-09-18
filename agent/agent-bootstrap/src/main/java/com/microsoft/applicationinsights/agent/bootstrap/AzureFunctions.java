// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap;

import java.util.Optional;
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
    Optional.ofNullable(configure)
            .ifPresent(result ->{
              if (!hasConnectionString()) 
              {
                result.run();
              }  
              result=null;
            });
  }

  private AzureFunctions() {}
}
