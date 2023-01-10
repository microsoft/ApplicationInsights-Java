// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

/**
 * {@code UploadFinishArgs} class contains information related to completed trace file upload
 * operation.
 *
 * <p>This class is intended for internal Java profiler use.
 */
public class UploadFinishArgs {

  private final String stampId;
  private final String timeStamp;

  public UploadFinishArgs(String stampId, String timeStamp) {
    this.stampId = stampId;
    this.timeStamp = timeStamp;
  }

  String getStampId() {
    return stampId;
  }

  String getTimeStamp() {
    return timeStamp;
  }
}
