package com.microsoft.applicationinsights.agent.internal.telemetry;

public class TelemetryPipelineResponse {

  private final int statusCode;
  private final String body;

  TelemetryPipelineResponse(int statusCode, String body) {
    this.statusCode = statusCode;
    this.body = body;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getBody() {
    return body;
  }
}
