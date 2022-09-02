// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.profiler.uploader;

public interface UploadCompleteHandler {
  void notify(UploadResult done);
}
