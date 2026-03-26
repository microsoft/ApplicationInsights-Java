// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

public class UnshadedSdkPackageName {

  // using package-prefix constants here so that they will NOT get shaded
  // IMPORTANT FOR THESE NOT TO BE FINAL (or private)
  // OTHERWISE COMPILER COULD THEORETICALLY INLINE THEM BELOW AND APPLY .substring(1)
  // and then they WOULD be shaded
  @SuppressWarnings("ConstantField") // field value intentionally mutable for specific use case
  static String ALMOST_PREFIX = "!com/microsoft/applicationinsights";

  @SuppressWarnings("ConstantField") // field value intentionally mutable for specific use case
  static String ALMOST_LOGBACK_PREFIX = "!ch/qos/";

  public static String get() {
    return ALMOST_PREFIX.substring(1);
  }

  public static String getLogbackPrefix() {
    return ALMOST_LOGBACK_PREFIX.substring(1);
  }

  private UnshadedSdkPackageName() {}
}
