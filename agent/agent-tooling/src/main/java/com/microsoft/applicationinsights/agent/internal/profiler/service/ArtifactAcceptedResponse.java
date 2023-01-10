// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

/** Result of uploading an artifact to service profiler. */
@AutoValue
public abstract class ArtifactAcceptedResponse {

  @JsonCreator
  public static ArtifactAcceptedResponse create(
      @JsonProperty("acceptedTime") String acceptedTime,
      @JsonProperty("blobUri") String blobUri,
      @JsonProperty("correlationId") String correlationId,
      @JsonProperty("stampId") String stampId) {

    return new AutoValue_ArtifactAcceptedResponse(acceptedTime, stampId, correlationId, blobUri);
  }

  public abstract String getAcceptedTime();

  public abstract String getStampId();

  public abstract String getCorrelationId();

  public abstract String getBlobUri();
}
