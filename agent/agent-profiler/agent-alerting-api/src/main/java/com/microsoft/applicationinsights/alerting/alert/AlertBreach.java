// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.alert;

import com.azure.json.JsonSerializable;
import com.azure.json.JsonWriter;
import com.google.auto.value.AutoValue;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.io.IOException;
import java.util.UUID;

/** Represents a breach of an alert threshold. */
@AutoValue
public abstract class AlertBreach implements JsonSerializable<AlertBreach> {

  private AlertMetricType type;
  private double alertValue;
  private AlertConfiguration alertConfiguration;
  private double cpuMetric;
  private double memoryUsage;
  private String profileId = UUID.randomUUID().toString();

  public abstract AlertMetricType getType();

  public AlertBreach setType(AlertMetricType type) {
    this.type = type;
    return this;
  }

  // Value of the telemetry at the time of the breach
  public abstract double getAlertValue();

  public AlertBreach setAlertValue(double alertValue) {
    this.alertValue = alertValue;
    return this;
  }

  public abstract AlertConfiguration getAlertConfiguration();

  public AlertBreach setAlertConfiguration(AlertConfiguration alertConfiguration) {
    this.alertConfiguration = alertConfiguration;
    return this;
  }

  // CPU usage at the time of the breach
  public abstract double getCpuMetric();

  public AlertBreach setCpuMetric(double cpuMetric) {
    this.cpuMetric = cpuMetric;
    return this;
  }

  // MEMORY usage at the time of the breach
  public abstract double getMemoryUsage();

  public AlertBreach setMemoryUsage(double memoryUsage) {
    this.memoryUsage = memoryUsage;
    return this;
  }

  // Unique ID for profile/breach
  public abstract String getProfileId();

  public AlertBreach setProfileId(String profileId) {
    this.profileId = profileId;
    return this;
  }

  public abstract Builder toBuilder();

  public static AlertBreach.Builder builder() {
    return new AutoValue_AlertBreach.Builder()
        .setCpuMetric(0)
        .setMemoryUsage(0)
        .setProfileId(UUID.randomUUID().toString());
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    jsonWriter.writeStartObject();
    jsonWriter.writeStringField("type", type.name());
    jsonWriter.writeDoubleField("alertValue", alertValue);
    jsonWriter.writeJsonField("alertConfiguration", alertConfiguration);
    jsonWriter.writeDoubleField("cpuMetric", cpuMetric);
    jsonWriter.writeDoubleField("memoryUsage", memoryUsage);
    jsonWriter.writeStringField("profileId", profileId);
    jsonWriter.writeEndObject();
    return jsonWriter;
  }

  @AutoValue.Builder
  public abstract static class Builder implements JsonSerializable<Builder> {
    private AlertMetricType type;
    private double alertValue;
    private AlertConfiguration alertConfiguration;
    private double cpuMetric;
    private double memoryUsage;
    private final String profileId = UUID.randomUUID().toString();

    public abstract Builder setType(AlertMetricType type);

    public abstract Builder setAlertValue(double alertValue);

    public abstract Builder setAlertConfiguration(AlertConfiguration alertConfiguration);

    public abstract Builder setCpuMetric(double cpuMetric);

    public abstract Builder setMemoryUsage(double memoryUsage);

    public abstract Builder setProfileId(String profileId);

    public abstract AlertBreach build();

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("type", type.name());
      jsonWriter.writeDoubleField("alertValue", alertValue);
      jsonWriter.writeJsonField("alertConfiguration", alertConfiguration);
      jsonWriter.writeDoubleField("cpuMetric", cpuMetric);
      jsonWriter.writeDoubleField("memoryUsage", memoryUsage);
      jsonWriter.writeStringField("profileId", profileId);
      jsonWriter.writeEndObject();
      return jsonWriter;
    }
  }
}
