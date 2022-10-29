// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.client;

import com.squareup.moshi.Json;

/** Result of uploading an artifact to service profiler. */
public class ArtifactAcceptedResponse {

  @Json(name = "acceptedTime")
  private final String acceptedTime;

  @Json(name = "blobUri")
  private final String blobUri;

  @Json(name = "correlationId")
  private final String correlationId;

  @Json(name = "stampId")
  private final String stampId;

  public ArtifactAcceptedResponse(
      String acceptedTime, String blobUri, String correlationId, String stampId) {
    this.acceptedTime = acceptedTime;
    this.blobUri = blobUri;
    this.correlationId = correlationId;
    this.stampId = stampId;
  }

  public String getAcceptedTime() {
    return acceptedTime;
  }

  public String getStampId() {
    return stampId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getBlobUri() {
    return blobUri;
  }
}
