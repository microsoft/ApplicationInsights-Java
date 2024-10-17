// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.aiconfig;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;

public class AlertingConfig {

  public enum RequestFilterType {
    NAME_REGEX
  }

  public static class RequestFilter implements JsonSerializable<RequestFilter> {
    public RequestFilterType type;
    public String value;

    public RequestFilterType getType() {
      return type;
    }

    public RequestFilter setType(RequestFilterType type) {
      this.type = type;
      return this;
    }

    public String getValue() {
      return value;
    }

    public RequestFilter setValue(String value) {
      this.value = value;
      return this;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("type", type.name());
      jsonWriter.writeStringField("value", value);
      jsonWriter.writeEndObject();
      return jsonWriter;
    }

    public static RequestFilter fromJson(JsonReader jsonReader) throws IOException {
      return jsonReader.readObject(
          reader -> {
            RequestFilter deserializedRequestFilter = new RequestFilter();
            while (reader.nextToken() != JsonToken.END_OBJECT) {
              reader.nextToken();
              String fieldName = reader.getFieldName();
              if ("type".equals(fieldName)) {
                deserializedRequestFilter.setType(RequestFilterType.valueOf(reader.getString()));
              } else if ("value".equals(fieldName)) {
                deserializedRequestFilter.setValue(reader.getString());
              } else {
                reader.skipChildren();
              }
            }
            return deserializedRequestFilter;
          });
    }
  }

  public static class RequestAggregationConfig
      implements JsonSerializable<RequestAggregationConfig> {

    // Threshold in ms over which a span will consider to be a breach
    // Used by the breach ratio aggregation
    public int thresholdMillis;

    // Minimum number of samples that must have been collected in order for the aggregation to
    // produce data. Avoids volatile aggregation output on small sample sizes.
    public int minimumSamples;

    public int getThresholdMillis() {
      return thresholdMillis;
    }

    public RequestAggregationConfig setThresholdMillis(int thresholdMillis) {
      this.thresholdMillis = thresholdMillis;
      return this;
    }

    public int getMinimumSamples() {
      return minimumSamples;
    }

    public RequestAggregationConfig setMinimumSamples(int minimumSamples) {
      this.minimumSamples = minimumSamples;
      return this;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeIntField("thresholdMillis", thresholdMillis);
      jsonWriter.writeIntField("minimumSamples", minimumSamples);
      jsonWriter.writeEndObject();
      return jsonWriter;
    }

    public static RequestAggregationConfig fromJson(JsonReader jsonReader) throws IOException {
      return jsonReader.readObject(
          reader -> {
            RequestAggregationConfig deserializedRequestAggregationConfig =
                new RequestAggregationConfig();
            while (reader.nextToken() != JsonToken.END_OBJECT) {
              reader.nextToken();
              String fieldName = reader.getFieldName();
              if ("thresholdMillis".equals(fieldName)) {
                deserializedRequestAggregationConfig.setThresholdMillis(jsonReader.getInt());
              } else if ("minimumSamples".equals(fieldName)) {
                deserializedRequestAggregationConfig.setMinimumSamples(jsonReader.getInt());
              } else {
                reader.skipChildren();
              }
            }
            return deserializedRequestAggregationConfig;
          });
    }
  }

  public enum RequestAggregationType {
    BREACH_RATIO
  }

  public static class RequestAggregation implements JsonSerializable<RequestAggregation> {
    public RequestAggregationType type;
    public long windowSizeMillis; // in ms
    public RequestAggregationConfig configuration;

    public RequestAggregationType getType() {
      return type;
    }

    public RequestAggregation setType(RequestAggregationType type) {
      this.type = type;
      return this;
    }

    public long getWindowSizeMillis() {
      return windowSizeMillis;
    }

    public RequestAggregation setWindowSizeMillis(long windowSizeMillis) {
      this.windowSizeMillis = windowSizeMillis;
      return this;
    }

    public RequestAggregationConfig getConfiguration() {
      return configuration;
    }

    public RequestAggregation setConfiguration(RequestAggregationConfig configuration) {
      this.configuration = configuration;
      return this;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("type", type.name());
      jsonWriter.writeLongField("windowSizeMillis", windowSizeMillis);
      jsonWriter.writeJsonField("configuration", configuration);
      jsonWriter.writeEndObject();
      return jsonWriter;
    }

    public static RequestAggregation fromJson(JsonReader jsonReader) throws IOException {
      return jsonReader.readObject(
          reader -> {
            RequestAggregation deserializedRequestAggregation = new RequestAggregation();
            while (reader.nextToken() != JsonToken.END_OBJECT) {
              reader.nextToken();
              String fieldName = reader.getFieldName();
              if ("type".equals(fieldName)) {
                deserializedRequestAggregation.setType(
                    RequestAggregationType.valueOf(reader.getString()));
              } else if ("windowSizeMillis".equals(fieldName)) {
                deserializedRequestAggregation.setWindowSizeMillis(jsonReader.getLong());
              } else if ("configuration".equals(fieldName)) {
                deserializedRequestAggregation.setConfiguration(
                    RequestAggregationConfig.fromJson(jsonReader));
              } else {
                reader.skipChildren();
              }
            }
            return deserializedRequestAggregation;
          });
    }
  }

  public enum RequestTriggerThresholdType {
    GREATER_THAN
  }

  public static class RequestTriggerThreshold implements JsonSerializable<RequestTriggerThreshold> {
    public RequestTriggerThresholdType type;

    // Threshold value applied to the output of the aggregation
    // i.e :
    //  - For the BreachRatio aggregation, 0.75 means this will trigger if 75% of sample breach the
    // threshold.
    //  - For a rolling average aggregation 0.75 will mean this will trigger if the average request
    // processing time
    //      breaches 0.75ms
    public float value;

    public RequestTriggerThresholdType getType() {
      return type;
    }

    public RequestTriggerThreshold setType(RequestTriggerThresholdType type) {
      this.type = type;
      return this;
    }

    public float getValue() {
      return value;
    }

    public RequestTriggerThreshold setValue(float value) {
      this.value = value;
      return this;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("type", type.name());
      jsonWriter.writeFloatField("value", value);
      jsonWriter.writeEndObject();
      return jsonWriter;
    }

    public static RequestTriggerThreshold fromJson(JsonReader jsonReader) throws IOException {
      return jsonReader.readObject(
          reader -> {
            RequestTriggerThreshold deserializedRequestTriggerThreshold =
                new RequestTriggerThreshold();
            while (reader.nextToken() != JsonToken.END_OBJECT) {
              reader.nextToken();
              String fieldName = reader.getFieldName();
              if ("type".equals(fieldName)) {
                deserializedRequestTriggerThreshold.setType(
                    RequestTriggerThresholdType.valueOf(reader.getString()));
              } else if ("value".equals(fieldName)) {
                deserializedRequestTriggerThreshold.setValue(reader.getFloat());
              } else {
                reader.skipChildren();
              }
            }
            return deserializedRequestTriggerThreshold;
          });
    }
  }

  public enum RequestTriggerThrottlingType {
    FIXED_DURATION_COOLDOWN
  }

  public static class RequestTriggerThrottling
      implements JsonSerializable<RequestTriggerThrottling> {
    public RequestTriggerThrottlingType type;
    public long value; // in seconds

    public RequestTriggerThrottlingType getType() {
      return type;
    }

    public RequestTriggerThrottling setType(RequestTriggerThrottlingType type) {
      this.type = type;
      return this;
    }

    public long getValue() {
      return value;
    }

    public RequestTriggerThrottling setValue(long value) {
      this.value = value;
      return this;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("type", type.name());
      jsonWriter.writeLongField("value", value);
      jsonWriter.writeEndObject();
      return jsonWriter;
    }

    public static RequestTriggerThrottling fromJson(JsonReader jsonReader) throws IOException {
      return jsonReader.readObject(
          reader -> {
            RequestTriggerThrottling deserializedRequestTriggerThrottling =
                new RequestTriggerThrottling();
            while (reader.nextToken() != JsonToken.END_OBJECT) {
              reader.nextToken();
              String fieldName = reader.getFieldName();
              if ("type".equals(fieldName)) {
                deserializedRequestTriggerThrottling.setType(
                    RequestTriggerThrottlingType.valueOf(reader.getString()));
              } else if ("value".equals(fieldName)) {
                deserializedRequestTriggerThrottling.setValue(reader.getLong());
              } else {
                reader.skipChildren();
              }
            }
            return deserializedRequestTriggerThrottling;
          });
    }
  }

  public enum RequestTriggerType {
    LATENCY
  }

  public static class RequestTrigger implements JsonSerializable<RequestTrigger> {
    public String name;
    public RequestTriggerType type;
    public RequestFilter filter;
    public RequestAggregation aggregation;
    public RequestTriggerThreshold threshold;
    public RequestTriggerThrottling throttling;
    public long profileDuration;

    public String getName() {
      return name;
    }

    public RequestTrigger setName(String name) {
      this.name = name;
      return this;
    }

    public RequestTriggerType getType() {
      return type;
    }

    public RequestTrigger setType(RequestTriggerType type) {
      this.type = type;
      return this;
    }

    public RequestFilter getFilter() {
      return filter;
    }

    public RequestTrigger setFilter(RequestFilter filter) {
      this.filter = filter;
      return this;
    }

    public RequestAggregation getAggregation() {
      return aggregation;
    }

    public RequestTrigger setAggregation(RequestAggregation aggregation) {
      this.aggregation = aggregation;
      return this;
    }

    public RequestTriggerThreshold getThreshold() {
      return threshold;
    }

    public RequestTrigger setThreshold(RequestTriggerThreshold threshold) {
      this.threshold = threshold;
      return this;
    }

    public RequestTriggerThrottling getThrottling() {
      return throttling;
    }

    public RequestTrigger setThrottling(RequestTriggerThrottling throttling) {
      this.throttling = throttling;
      return this;
    }

    public long getProfileDuration() {
      return profileDuration;
    }

    public RequestTrigger setProfileDuration(long profileDuration) {
      this.profileDuration = profileDuration;
      return this;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("name", name);
      jsonWriter.writeStringField("type", type.name());
      jsonWriter.writeJsonField("filter", filter);
      jsonWriter.writeJsonField("aggregation", aggregation);
      jsonWriter.writeJsonField("threshold", threshold);
      jsonWriter.writeJsonField("throttling", throttling);
      jsonWriter.writeLongField("profileDuration", profileDuration);
      jsonWriter.writeEndObject();
      return jsonWriter;
    }

    public static RequestTrigger fromJson(JsonReader jsonReader) throws IOException {
      return jsonReader.readObject(
          reader -> {
            RequestTrigger deserializedRequestTrigger = new RequestTrigger();
            while (reader.nextToken() != JsonToken.END_OBJECT) {
              reader.nextToken();
              String fieldName = reader.getFieldName();
              if ("name".equals(fieldName)) {
                deserializedRequestTrigger.setName(reader.getString());
              } else if ("type".equals(fieldName)) {
                deserializedRequestTrigger.setType(RequestTriggerType.valueOf(reader.getString()));
              } else if ("filter".equals(fieldName)) {
                deserializedRequestTrigger.setFilter(RequestFilter.fromJson(reader));
              } else if ("aggregation".equals(fieldName)) {
                deserializedRequestTrigger.setAggregation(RequestAggregation.fromJson(reader));
              } else if ("threshold".equals(fieldName)) {
                deserializedRequestTrigger.setThreshold(RequestTriggerThreshold.fromJson(reader));
              } else if ("throttling".equals(fieldName)) {
                deserializedRequestTrigger.setThrottling(RequestTriggerThrottling.fromJson(reader));
              } else if ("profileDuration".equals(fieldName)) {
                deserializedRequestTrigger.setProfileDuration(reader.getLong());
              } else {
                reader.skipChildren();
              }
            }
            return deserializedRequestTrigger;
          });
    }
  }

  private AlertingConfig() {}
}
