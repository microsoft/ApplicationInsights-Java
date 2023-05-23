package com.microsoft.applicationinsights.alerting.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.util.ArrayList;
import java.util.List;

@AutoValue
@JsonSerialize(as = RequestTriggerConfiguration.class)
@JsonDeserialize(builder = RequestTriggerConfiguration.Builder.class)
public abstract class RequestTriggerConfiguration {
  @JsonProperty("requestTriggerEndpoints")
  public abstract List<AlertingConfig.RequestTrigger> getRequestTriggerEndpoints();

  public static Builder builder() {
    return new AutoValue_RequestTriggerConfiguration.Builder()
        .setRequestTriggerEndpoints(new ArrayList<>());
  }

  public static Builder builder(List<AlertingConfig.RequestTrigger> requestTriggerEndpoints) {
    return new AutoValue_RequestTriggerConfiguration.Builder()
            .setRequestTriggerEndpoints(requestTriggerEndpoints);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonCreator
    public static Builder builder() {
      return RequestTriggerConfiguration.builder();
    }

    @JsonProperty("requestTriggerEndpoints")
    public abstract Builder setRequestTriggerEndpoints(List<AlertingConfig.RequestTrigger> requestTriggerEndpoints);

    public abstract RequestTriggerConfiguration build();
  }
}
