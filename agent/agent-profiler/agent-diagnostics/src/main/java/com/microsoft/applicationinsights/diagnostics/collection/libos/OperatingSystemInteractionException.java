// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos;

public class OperatingSystemInteractionException extends Exception {

  public OperatingSystemInteractionException(String message, Throwable cause) {
    super(message, cause);
  }

  public OperatingSystemInteractionException(String message) {
    super(message);
  }

  public OperatingSystemInteractionException(Throwable cause) {
    super(cause);
  }
}
