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

package com.microsoft.applicationinsights.agent.internal.quickpulse;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.applicationinsights.agent.internal.common.ExceptionUtils;
import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.init.MainEntryPoint;
import com.microsoft.applicationinsights.agent.internal.quickpulse.model.QuickPulseEnvelope;
import com.microsoft.applicationinsights.agent.internal.quickpulse.util.CustomCharacterEscapes;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class QuickPulsePingSender {

  private static final Logger logger = LoggerFactory.getLogger(QuickPulsePingSender.class);

  private static final ObjectMapper mapper;

  private static final String quickPulseVersion = MainEntryPoint.getAgentVersion();

  private static final OperationLogger operationLogger =
      new OperationLogger(QuickPulsePingSender.class, "Pinging live metrics endpoint");

  // TODO (kryalama) do we still need this AtomicBoolean, or can we use throttling built in to the
  //  operationLogger?
  private static final AtomicBoolean friendlyExceptionThrown = new AtomicBoolean();

  static {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.getFactory().setCharacterEscapes(new CustomCharacterEscapes());
  }

  private final TelemetryClient telemetryClient;
  private final HttpPipeline httpPipeline;
  private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
  private volatile QuickPulseEnvelope pingEnvelope; // cached for performance
  private final String instanceName;
  private final String machineName;
  private final String quickPulseId;
  private long lastValidTransmission = 0;

  public QuickPulsePingSender(
      HttpPipeline httpPipeline,
      TelemetryClient telemetryClient,
      String machineName,
      String instanceName,
      String quickPulseId) {
    this.telemetryClient = telemetryClient;
    this.httpPipeline = httpPipeline;
    this.instanceName = instanceName;
    this.machineName = machineName;
    this.quickPulseId = quickPulseId;
    if (logger.isTraceEnabled()) {
      logger.trace(
          "{} using endpoint {}",
          QuickPulsePingSender.class.getSimpleName(),
          getQuickPulseEndpoint());
    }
  }

  public QuickPulseHeaderInfo ping(String redirectedEndpoint) {
    String instrumentationKey = getInstrumentationKey();
    if (instrumentationKey == null && "java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
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
            telemetryClient.getRoleName(),
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
      operationLogger.recordFailure(t.getMessage(), t);
      ExceptionUtils.parseError(t, getQuickPulseEndpoint(), friendlyExceptionThrown, logger);
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
    return telemetryClient.getInstrumentationKey();
  }

  // visible for testing
  String getQuickPulseEndpoint() {
    return telemetryClient.getEndpointProvider().getLiveEndpointUrl().toString();
  }

  private String buildPingEntity(long timeInMillis) throws JsonProcessingException {
    if (pingEnvelope == null) {
      pingEnvelope = new QuickPulseEnvelope();
      pingEnvelope.setInstance(instanceName);
      pingEnvelope.setInvariantVersion(QuickPulse.QP_INVARIANT_VERSION);
      pingEnvelope.setMachineName(machineName);
      pingEnvelope.setRoleName(telemetryClient.getRoleName());
      pingEnvelope.setStreamId(quickPulseId);
      pingEnvelope.setVersion(quickPulseVersion);
    }
    pingEnvelope.setTimeStamp("/Date(" + timeInMillis + ")/");
    String envelope = mapper.writeValueAsString(pingEnvelope);
    return envelope;
  }

  private QuickPulseHeaderInfo onPingError(long sendTime) {
    double timeFromLastValidTransmission = (sendTime - lastValidTransmission) / 1000000000.0;
    if (timeFromLastValidTransmission >= 60.0) {
      return new QuickPulseHeaderInfo(QuickPulseStatus.ERROR);
    }

    return new QuickPulseHeaderInfo(QuickPulseStatus.QP_IS_OFF);
  }
}
