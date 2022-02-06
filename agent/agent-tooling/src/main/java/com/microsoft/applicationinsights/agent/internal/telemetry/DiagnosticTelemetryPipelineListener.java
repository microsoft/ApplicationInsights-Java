package com.microsoft.applicationinsights.agent.internal.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DiagnosticTelemetryPipelineListener implements TelemetryPipelineListener {

  private final OperationLogger operationLogger;

  // e.g. "Sending telemetry to the ingestion service"
  public DiagnosticTelemetryPipelineListener(String operation) {
    operationLogger = new OperationLogger(TelemetryPipeline.class, operation);
  }

  @Override
  public void onResponse(
      int responseCode,
      String responseBody,
      List<ByteBuffer> requestBody,
      String instrumentationKey) {
    switch (responseCode) {
      case 200: // SUCCESS
        operationLogger.recordSuccess();
        break;
      case 206: // PARTIAL CONTENT, Breeze-specific: PARTIAL SUCCESS
        operationLogger.recordFailure(getErrorMessageFromPartialSuccessResponse(responseBody));
        break;
      case 401: // breeze returns if aad enabled and no authentication token provided
      case 403: // breeze returns if aad enabled or disabled (both cases) and
        // wrong/expired credentials provided
        operationLogger.recordFailure(
            getErrorMessageFromCredentialRelatedResponse(responseCode, responseBody));
        break;
      case 408: // REQUEST TIMEOUT
      case 429: // TOO MANY REQUESTS
      case 500: // INTERNAL SERVER ERROR
      case 503: // SERVICE UNAVAILABLE
        operationLogger.recordFailure(
            "received response code "
                + responseCode
                + " (telemetry will be stored to disk and retried later)");
        break;
      case 439: // Breeze-specific: THROTTLED OVER EXTENDED TIME
        // TODO handle throttling
        operationLogger.recordFailure("received response code 439 (throttled over extended time)");
        break;
      default:
        operationLogger.recordFailure("received response code: " + responseCode);
    }
  }

  @Override
  public void onError(
      String reason, Throwable error, List<ByteBuffer> requestBody, String instrumentationKey) {
    // FIXME (trask) handle one time friendly network error
    operationLogger.recordFailure("Error sending telemetry items: " + error.getMessage(), error);
  }

  private static String getErrorMessageFromPartialSuccessResponse(String body) {
    JsonNode jsonNode;
    try {
      jsonNode = new ObjectMapper().readTree(body);
    } catch (JsonProcessingException e) {
      return "ingestion service returned 206, but could not parse response as json: " + body;
    }
    List<JsonNode> errors = new ArrayList<>();
    jsonNode.get("errors").forEach(errors::add);
    StringBuilder message = new StringBuilder();
    message.append(errors.get(0).get("message").asText());
    int moreErrors = errors.size() - 1;
    if (moreErrors > 0) {
      message.append(" (and ").append(moreErrors).append(" more)");
    }
    return message.toString();
  }

  private static String getErrorMessageFromCredentialRelatedResponse(int statusCode, String body) {
    JsonNode jsonNode;
    try {
      jsonNode = new ObjectMapper().readTree(body);
    } catch (JsonProcessingException e) {
      return "ingestion service returned "
          + statusCode
          + ", but could not parse response as json: "
          + body;
    }
    String action =
        statusCode == 401
            ? ". Please provide Azure Active Directory credentials"
            : ". Please check your Azure Active Directory credentials, they might be incorrect or expired";
    List<JsonNode> errors = new ArrayList<>();
    jsonNode.get("errors").forEach(errors::add);
    return errors.get(0).get("message").asText()
        + action
        + " (telemetry will be stored to disk and retried later)";
  }
}
