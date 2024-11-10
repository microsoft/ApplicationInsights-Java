// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.service;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;

/** Result of uploading an artifact to service profiler. */
public class ArtifactAcceptedResponse implements JsonSerializable<ArtifactAcceptedResponse> {

  private String acceptedTime;
  private String stampId;
  private String correlationId;
  private String blobUri;

  public String getAcceptedTime() {
    return acceptedTime;
  }

  public ArtifactAcceptedResponse setAcceptedTime(String acceptedTime) {
    this.acceptedTime = acceptedTime;
    return this;
  }

  public String getStampId() {
    return stampId;
  }

  public ArtifactAcceptedResponse setStampId(String stampId) {
    this.stampId = stampId;
    return this;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public ArtifactAcceptedResponse setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
    return this;
  }

  public String getBlobUri() {
    return blobUri;
  }

  public ArtifactAcceptedResponse setBlobUri(String blobUri) {
    this.blobUri = blobUri;
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    jsonWriter.writeStartObject();
    jsonWriter.writeStringField("acceptedTime", acceptedTime);
    jsonWriter.writeStringField("stampId", stampId);
    jsonWriter.writeStringField("correlationId", correlationId);
    jsonWriter.writeStringField("blobUri", blobUri);
    jsonWriter.writeEndObject();
    return jsonWriter;
  }

  public static ArtifactAcceptedResponse fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        reader -> {
          ArtifactAcceptedResponse deserializedArtifactAcceptedResponse =
              new ArtifactAcceptedResponse();
          while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            if ("acceptedTime".equals(fieldName)) {
              deserializedArtifactAcceptedResponse.setAcceptedTime(reader.getString());
            } else if ("stampId".equals(fieldName)) {
              deserializedArtifactAcceptedResponse.setStampId(reader.getString());
            } else if ("correlationId".equals(fieldName)) {
              deserializedArtifactAcceptedResponse.setCorrelationId(reader.getString());
            } else if ("blobUri".equals(fieldName)) {
              deserializedArtifactAcceptedResponse.setBlobUri(reader.getString());
            } else {
              reader.skipChildren();
            }
          }
          return deserializedArtifactAcceptedResponse;
        });
  }
}
