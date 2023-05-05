// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos;

public class UnsupportedPlatformException extends RuntimeException {

  public UnsupportedPlatformException(String message) {
    super(message);
  }
}
