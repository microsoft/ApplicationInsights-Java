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

package com.azure.monitor.opentelemetry.exporter.implementation.quickpulse;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.model.QuickPulseEnvelope;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.util.CustomCharacterEscapes;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class QuickPulsePingSender {

  private static final Logger logger = LoggerFactory.getLogger(QuickPulsePingSender.class);
  private static final String QP_BASE_URI =
      "https://rt.services.visualstudio.com/QuickPulseService.svc";
  private static final String LIVE_URL_PATH = "QuickPulseService.svc";

  private static final ObjectMapper mapper;

  // TODO Kishna populate this
  private static final String quickPulseVersion = "(unknown)";

  private static final OperationLogger operationLogger =
      new OperationLogger(QuickPulsePingSender.class, "Pinging live metrics endpoint");

  static {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.getFactory().setCharacterEscapes(new CustomCharacterEscapes());
  }

  private final HttpPipeline httpPipeline;
  private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
  private volatile QuickPulseEnvelope pingEnvelope; // cached for performance
  private final String instanceName;
  private final String machineName;
  private final String quickPulseId;
  private final String roleName;
  private final String instrumentationKey;
  private final String endPointUrl;
  private long lastValidTransmission = 0;

  public QuickPulsePingSender(
      HttpPipeline httpPipeline,
      String roleName,
      String instrumentationKey,
      String machineName,
      String instanceName,
      String quickPulseId,
      String endPointUrl) {
    this.httpPipeline = httpPipeline;
    this.instanceName = instanceName;
    this.machineName = machineName;
    this.quickPulseId = quickPulseId;
    this.roleName = roleName;
    this.instrumentationKey = instrumentationKey;
    this.endPointUrl = endPointUrl;
    if (logger.isTraceEnabled()) {
      logger.trace(
          "{} using endpoint {}",
          QuickPulsePingSender.class.getSimpleName(),
          getQuickPulseEndpoint());
    }
  }

  public QuickPulseHeaderInfo ping(String redirectedEndpoint) {
    String instrumentationKey = getInstrumentationKey();
    if (Strings.isNullOrEmpty(instrumentationKey)) {
      // Quick Pulse Ping uri will be null when the instrumentation key is null. When that happens,
      // turn off quick pulse.
      return new QuickPulseHeaderInfo(QuickPulseStatus.QP_IS_OFF);
    }

    Date currentDate = new Date();
    String endpointPrefix =
        Strings.isNullOrEmpty(redirectedEndpoint) ? getQuickPulseEndpoint() : redirectedEndpoint;
    HttpRequest request =
        networkHelper.buildPingRequest(
            currentDate,
            getQuickPulsePingUri(endpointPrefix),
            quickPulseId,
            machineName,
            roleName,
            instanceName);

    long sendTime = System.nanoTime();
    HttpResponse response = null;
    try {
      request.setBody(buildPingEntity(currentDate.getTime()));
      response = httpPipeline.send(request).block();
      if (response == null) {
        // this shouldn't happen, the mono should complete with a response or a failure
        throw new AssertionError("http response mono returned empty");
      }
      // response body is not consumed below
      LazyHttpClient.consumeResponseBody(response);

      if (networkHelper.isSuccess(response)) {
        QuickPulseHeaderInfo quickPulseHeaderInfo = networkHelper.getQuickPulseHeaderInfo(response);
        switch (quickPulseHeaderInfo.getQuickPulseStatus()) {
          case QP_IS_OFF:
          case QP_IS_ON:
            lastValidTransmission = sendTime;
            operationLogger.recordSuccess();
            return quickPulseHeaderInfo;

          default:
            break;
        }
      }
    } catch (Throwable t) {
      logger.warn(t.getMessage(), t);
    } finally {
      if (response != null) {
        response.close();
      }
    }
    return onPingError(sendTime);
  }

  // visible for testing
  String getQuickPulsePingUri(String endpointPrefix) {
    return endpointPrefix + "/ping?ikey=" + getInstrumentationKey();
  }

  private String getInstrumentationKey() {
    return instrumentationKey;
  }

  // visible for testing
  String getQuickPulseEndpoint() {
    return endPointUrl == null ? QP_BASE_URI : endPointUrl + LIVE_URL_PATH;
  }

  private String buildPingEntity(long timeInMillis) throws JsonProcessingException {
    if (pingEnvelope == null) {
      pingEnvelope = new QuickPulseEnvelope();
      pingEnvelope.setInstance(instanceName);
      pingEnvelope.setInvariantVersion(QuickPulse.QP_INVARIANT_VERSION);
      pingEnvelope.setMachineName(machineName);
      pingEnvelope.setRoleName(roleName);
      pingEnvelope.setStreamId(quickPulseId);
      pingEnvelope.setVersion(quickPulseVersion);
    }
    pingEnvelope.setTimeStamp("/Date(" + timeInMillis + ")/");
    return mapper.writeValueAsString(pingEnvelope);
  }

  private QuickPulseHeaderInfo onPingError(long sendTime) {
    double timeFromLastValidTransmission = (sendTime - lastValidTransmission) / 1000000000.0;
    if (timeFromLastValidTransmission >= 60.0) {
      return new QuickPulseHeaderInfo(QuickPulseStatus.ERROR);
    }

    return new QuickPulseHeaderInfo(QuickPulseStatus.QP_IS_OFF);
  }
}
