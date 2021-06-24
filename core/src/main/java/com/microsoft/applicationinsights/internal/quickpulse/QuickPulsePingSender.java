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

package com.microsoft.applicationinsights.internal.quickpulse;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.exceptions.FriendlyException;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class QuickPulsePingSender {

  private static final Logger logger = LoggerFactory.getLogger(QuickPulsePingSender.class);

  private final TelemetryClient telemetryClient;
  private final HttpPipeline httpPipeline;
  private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
  private volatile String pingPrefix; // cached for performance
  private final String instanceName;
  private final String machineName;
  private final String quickPulseId;
  private long lastValidTransmission = 0;
  private static final AtomicBoolean friendlyExceptionThrown = new AtomicBoolean();

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
        LocalStringsUtils.isNullOrEmpty(redirectedEndpoint)
            ? getQuickPulseEndpoint()
            : redirectedEndpoint;
    HttpRequest request =
        networkHelper.buildPingRequest(
            currentDate,
            getQuickPulsePingUri(endpointPrefix),
            quickPulseId,
            machineName,
            telemetryClient.getRoleName(),
            instanceName);
    request.setBody(buildPingEntity(currentDate.getTime()));

    long sendTime = System.nanoTime();
    HttpResponse response = null;
    try {

      response = httpPipeline.send(request).block();
      if (response != null && networkHelper.isSuccess(response)) {
        QuickPulseHeaderInfo quickPulseHeaderInfo = networkHelper.getQuickPulseHeaderInfo(response);
        switch (quickPulseHeaderInfo.getQuickPulseStatus()) {
          case QP_IS_OFF:
          case QP_IS_ON:
            lastValidTransmission = sendTime;
            return quickPulseHeaderInfo;

          default:
            break;
        }
      }
    } catch (FriendlyException e) {
      if (!friendlyExceptionThrown.getAndSet(true)) {
        logger.error(e.getMessage());
      }
    } finally {
      if (response != null) {
        response.close();
      }
    }
    return onPingError(sendTime);
  }

  private String getPingPrefix() {
    if (pingPrefix == null) {
      // Linux Consumption Plan role name is lazily set
      String roleName = telemetryClient.getRoleName();

      StringBuilder sb = new StringBuilder();

      sb.append("{");
      sb.append("\"Documents\":null,");
      sb.append("\"Instance\":\"").append(instanceName).append("\",");
      sb.append("\"InstrumentationKey\":null,");
      sb.append("\"InvariantVersion\":").append(QuickPulse.QP_INVARIANT_VERSION).append(",");
      sb.append("\"MachineName\":\"").append(machineName).append("\",");
      if (LocalStringsUtils.isNullOrEmpty(roleName)) {
        sb.append("\"RoleName\":null,");
      } else {
        sb.append("\"RoleName\":\"").append(roleName).append("\",");
      }
      sb.append("\"StreamId\":\"").append(quickPulseId).append("\",");
      sb.append("\"Metrics\":null,");
      sb.append("\"Timestamp\":\"\\/Date(");

      pingPrefix = sb.toString();
    }
    return pingPrefix;
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

  private String buildPingEntity(long timeInMillis) {
    return getPingPrefix() + timeInMillis + ")\\/\"," + "\"Version\":\"2.2.0-738\"" + "}";
  }

  private QuickPulseHeaderInfo onPingError(long sendTime) {
    double timeFromLastValidTransmission = (sendTime - lastValidTransmission) / 1000000000.0;
    if (timeFromLastValidTransmission >= 60.0) {
      return new QuickPulseHeaderInfo(QuickPulseStatus.ERROR);
    }

    return new QuickPulseHeaderInfo(QuickPulseStatus.QP_IS_OFF);
  }
}
