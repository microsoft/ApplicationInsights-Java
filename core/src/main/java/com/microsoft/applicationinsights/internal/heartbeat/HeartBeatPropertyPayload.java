package com.microsoft.applicationinsights.internal.heartbeat;

/**
 * <p>
 *  Defines the Payload class to store and send heartbeat properties and allowing to keep track of updates
 *  to them.
 *  </p>
 */
public class HeartBeatPropertyPayload {

  /**
   * Value of payload on initialization. Ready for transmission.
   */
  private String payloadValue = "";

  /**
   * Is this healthy property or not.
   */
  private boolean isHealthy = false;

  /**
   * Returns the payload value
   * @return String value of payload property
   */
  String getPayloadValue() {
    return payloadValue;
  }

  /**
   * This is used to set the payload
   * @param payloadValue value of the property
   */
  public void setPayloadValue(String payloadValue) {
    if (payloadValue != null && !this.payloadValue.equals(payloadValue)) {
      this.payloadValue = payloadValue;
    }

  }

  /**
   * Gets the value of payload is healthy
   * @return returns true of payload value is healthy.
   */
  public boolean isHealthy() {
    return isHealthy;
  }

  /**
   * Sets the health of the payload.
   * @param healthy boolean value representing the health.
   */
  public void setHealthy(boolean healthy) {
    this.isHealthy = healthy;
  }
}
