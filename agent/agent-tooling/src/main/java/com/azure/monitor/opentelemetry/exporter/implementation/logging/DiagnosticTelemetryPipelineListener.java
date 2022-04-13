/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.azure.monitor.opentelemetry.exporter.implementation.logging;

import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipeline;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineRequest;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagnosticTelemetryPipelineListener implements TelemetryPipelineListener {

  private static final Class<?> FOR_CLASS = TelemetryPipeline.class;
  private static final Logger logger = LoggerFactory.getLogger(FOR_CLASS);

  // share this across multiple pipelines
  private static final AtomicBoolean friendlyExceptionThrown = new AtomicBoolean();

  private final OperationLogger operationLogger;
  private final boolean shouldLogWarnings;

  // e.g. "Sending telemetry to the ingestion service"
  public DiagnosticTelemetryPipelineListener(String operation, boolean shouldLogWarnings) {
    operationLogger = new OperationLogger(FOR_CLASS, operation);
    this.shouldLogWarnings = shouldLogWarnings;
  }

  @Override
  public void onResponse(TelemetryPipelineRequest request, TelemetryPipelineResponse response) {
    switch (response.getStatusCode()) {
      case 200: // SUCCESS
        operationLogger.recordSuccess();
        break;
      case 206: // PARTIAL CONTENT, Breeze-specific: PARTIAL SUCCESS
        operationLogger.recordFailure(
            getErrorMessageFromPartialSuccessResponse(response.getBody()));
        break;
      case 307:
      case 308:
        operationLogger.recordFailure("Too many redirects");
        break;
      case 401: // breeze returns if aad enabled and no authentication token provided
      case 403: // breeze returns if aad enabled or disabled (both cases) and
        if (shouldLogWarnings) {
          operationLogger.recordFailure(
              getErrorMessageFromCredentialRelatedResponse(
                  response.getStatusCode(), response.getBody()));
        }
        break;
      case 408: // REQUEST TIMEOUT
      case 429: // TOO MANY REQUESTS
      case 500: // INTERNAL SERVER ERROR
      case 503: // SERVICE UNAVAILABLE
        if (shouldLogWarnings) {
          operationLogger.recordFailure(
              "received response code "
                  + response.getStatusCode()
                  + " (telemetry will be stored to disk and retried later)");
        }
        break;
      case 402: // Breeze-specific: New Daily Quota Exceeded
        operationLogger.recordFailure(
            "received response code 402 (daily quota exceeded and throttled over extended time)");
        break;
      case 439: // Breeze-specific: Deprecated Daily Quota Exceeded
        operationLogger.recordFailure(
            "received response code 439 (daily quota exceeded and throttled over extended time)");
        break;
      default:
        operationLogger.recordFailure("received response code: " + response.getStatusCode());
    }
  }

  @Override
  public void onException(TelemetryPipelineRequest request, String reason, Throwable throwable) {
    if (!NetworkFriendlyExceptions.logSpecialOneTimeFriendlyException(
        throwable, request.getUrl().toString(), friendlyExceptionThrown, logger)) {
      operationLogger.recordFailure(reason, throwable);
    }
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

  private static String getErrorMessageFromCredentialRelatedResponse(
      int responseCode, String responseBody) {
    JsonNode jsonNode;
    try {
      jsonNode = new ObjectMapper().readTree(responseBody);
    } catch (JsonProcessingException e) {
      return "ingestion service returned "
          + responseCode
          + ", but could not parse response as json: "
          + responseBody;
    }
    String action =
        responseCode == 401
            ? ". Please provide Azure Active Directory credentials"
            : ". Please check your Azure Active Directory credentials, they might be incorrect or expired";
    List<JsonNode> errors = new ArrayList<>();
    jsonNode.get("errors").forEach(errors::add);
    return errors.get(0).get("message").asText()
        + action
        + " (telemetry will be stored to disk and retried later)";
  }
}
