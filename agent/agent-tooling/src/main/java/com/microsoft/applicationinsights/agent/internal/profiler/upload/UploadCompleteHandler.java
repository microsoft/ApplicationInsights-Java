// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

public interface UploadCompleteHandler {
  void notify(ServiceProfilerIndex serviceProfilerIndex);
}
