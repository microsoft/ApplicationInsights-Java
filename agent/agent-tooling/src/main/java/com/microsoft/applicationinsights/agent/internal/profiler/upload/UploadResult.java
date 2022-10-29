// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

/** Represents the metadata produced as a result of having uploaded a profile. */
public class UploadResult {
  private final ServiceProfilerIndex serviceProfilerIndex;
  private final long timestamp;

  public UploadResult(ServiceProfilerIndex serviceProfilerIndex, long timestamp) {
    this.serviceProfilerIndex = serviceProfilerIndex;
    this.timestamp = timestamp;
  }

  public ServiceProfilerIndex getServiceProfilerIndex() {
    return serviceProfilerIndex;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
