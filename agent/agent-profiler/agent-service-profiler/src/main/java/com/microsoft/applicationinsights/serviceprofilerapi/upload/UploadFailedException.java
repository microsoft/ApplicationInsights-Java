// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.upload;

public class UploadFailedException extends Exception {
  public UploadFailedException(Exception cause) {
    super(cause);
  }

  public UploadFailedException(String message) {
    super(message);
  }
}
