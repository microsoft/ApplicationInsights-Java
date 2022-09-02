// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

public class UnshadedSdkPackageName {

  // using constant here so that it will NOT get shaded
  // IMPORTANT FOR THIS NOT TO BE FINAL (or private)
  // OTHERWISE COMPILER COULD THEORETICALLY INLINE IT BELOW AND APPLY .substring(1)
  // and then it WOULD be shaded
  @SuppressWarnings("ConstantField")
  static String ALMOST_PREFIX = "!com/microsoft/applicationinsights";

  public static String get() {
    return ALMOST_PREFIX.substring(1);
  }

  private UnshadedSdkPackageName() {}
}
