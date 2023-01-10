// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

@FunctionalInterface
public interface UploadListener {
  void onUpload(ServiceProfilerIndex serviceProfilerIndex);
}
