// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

class UploadFailedException extends Exception {
  UploadFailedException(Exception cause) {
    super(cause);
  }

  UploadFailedException(String message) {
    super(message);
  }
}
