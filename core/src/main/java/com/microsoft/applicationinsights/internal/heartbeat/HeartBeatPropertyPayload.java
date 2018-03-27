package com.microsoft.applicationinsights.internal.heartbeat;

public class HeartBeatPropertyPayload {

  private String payloadValue = "";

  private boolean isHealthy = true;

  private boolean isUpdated = true;

  public String getPayloadValue() {
    return payloadValue;
  }

  public boolean isUpdated() {
    return isUpdated;
  }

  public void setUpdated(boolean updated) {
    isUpdated = updated;
  }

  public void setPayloadValue(String payloadValue) {
    if (payloadValue != null && !this.payloadValue.equals(payloadValue)) {
      this.payloadValue = payloadValue;
      isUpdated = true;
    }

  }

  public boolean isHealthy() {
    return isHealthy;
  }

  public void setHealthy(boolean healthy) {
    this.isUpdated = this.isHealthy != healthy;
    this.isHealthy = healthy;
  }
}
