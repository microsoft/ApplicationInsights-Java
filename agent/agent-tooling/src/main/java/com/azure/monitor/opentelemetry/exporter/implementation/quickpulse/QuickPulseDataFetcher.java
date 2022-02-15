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

import com.azure.core.http.HttpRequest;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.model.QuickPulseEnvelope;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.model.QuickPulseMetrics;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.util.CustomCharacterEscapes;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class QuickPulseDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(QuickPulseDataFetcher.class);

  private static final String QP_BASE_URI =
      "https://rt.services.visualstudio.com/QuickPulseService.svc";

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.getFactory().setCharacterEscapes(new CustomCharacterEscapes());
  }

  private final ArrayBlockingQueue<HttpRequest> sendQueue;
  private final String roleName;
  private final String instrumentationKey;
  private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
  private final String sdkVersion;
  private final String instanceName;
  private final String machineName;
  private final String quickPulseId;
  private final String endPointUrl;

  public QuickPulseDataFetcher(
      ArrayBlockingQueue<HttpRequest> sendQueue,
      String roleName,
      String instrumentationKey,
      String machineName,
      String instanceName,
      String quickPulseId,
      String endPointUrl) {
    this.sendQueue = sendQueue;
    this.roleName = roleName;
    this.instrumentationKey = instrumentationKey;
    this.instanceName = instanceName;
    this.machineName = machineName;
    this.quickPulseId = quickPulseId;
    this.endPointUrl = endPointUrl;
    sdkVersion = getCurrentSdkVersion();
    if (logger.isTraceEnabled()) {
      logger.trace(
          "{} using endpoint {}",
          QuickPulseDataFetcher.class.getSimpleName(),
          getQuickPulseEndpoint());
    }
  }

  /** Returns SDK Version from properties. */
  // visible for testing
  // TODO krishna to get sdk version
  String getCurrentSdkVersion() {
    return "unknown";
  }

  public void prepareQuickPulseDataForSend(String redirectedEndpoint) {
    try {
      QuickPulseDataCollector.FinalCounters counters =
          QuickPulseDataCollector.INSTANCE.getAndRestart();

      Date currentDate = new Date();
      String endpointPrefix =
          Strings.isNullOrEmpty(redirectedEndpoint) ? getQuickPulseEndpoint() : redirectedEndpoint;
      HttpRequest request =
          networkHelper.buildRequest(currentDate, this.getEndpointUrl(endpointPrefix));
      request.setBody(buildPostEntity(counters));

      if (!sendQueue.offer(request)) {
        logger.trace("Quick Pulse send queue is full");
      }
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable e) {
      try {
        logger.error("Quick Pulse failed to prepare data for send", e);
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t2) {
        // chomp
      }
    }
  }

  // visible for testing
  String getEndpointUrl(String endpointPrefix) {
    return endpointPrefix + "/post?ikey=" + getInstrumentationKey();
  }

  // visible for testing
  String getQuickPulseEndpoint() {
    return endPointUrl == null ? QP_BASE_URI : endPointUrl;
  }

  private String getInstrumentationKey() {
    return instrumentationKey;
  }

  private String buildPostEntity(QuickPulseDataCollector.FinalCounters counters)
      throws JsonProcessingException {
    List<QuickPulseEnvelope> envelopes = new ArrayList<>();
    QuickPulseEnvelope postEnvelope = new QuickPulseEnvelope();
    postEnvelope.setDocuments(counters.documentList);
    postEnvelope.setInstance(instanceName);
    postEnvelope.setInvariantVersion(QuickPulse.QP_INVARIANT_VERSION);
    postEnvelope.setMachineName(machineName);
    // FIXME (heya) what about azure functions consumption plan where role name not available yet?
    postEnvelope.setRoleName(this.roleName);
    // For historical reasons, instrumentation key is provided both in the query string and
    // envelope.
    postEnvelope.setInstrumentationKey(getInstrumentationKey());
    postEnvelope.setStreamId(quickPulseId);
    postEnvelope.setVersion(sdkVersion);
    postEnvelope.setTimeStamp("/Date(" + System.currentTimeMillis() + ")/");
    postEnvelope.setMetrics(addMetricsToQuickPulseEnvelope(counters));
    envelopes.add(postEnvelope);
    return mapper.writeValueAsString(envelopes);
  }

  private static List<QuickPulseMetrics> addMetricsToQuickPulseEnvelope(
      QuickPulseDataCollector.FinalCounters counters) {
    List<QuickPulseMetrics> metricsList = new ArrayList<>();
    metricsList.add(
        new QuickPulseMetrics("\\ApplicationInsights\\Requests/Sec", counters.requests, 1));
    metricsList.add(
        new QuickPulseMetrics(
            "\\ApplicationInsights\\Request Duration",
            (long) counters.requestsDuration,
            (int) counters.requests));
    metricsList.add(
        new QuickPulseMetrics(
            "\\ApplicationInsights\\Requests Failed/Sec", counters.unsuccessfulRequests, 1));
    metricsList.add(
        new QuickPulseMetrics(
            "\\ApplicationInsights\\Requests Succeeded/Sec",
            counters.requests - counters.unsuccessfulRequests,
            1));
    metricsList.add(
        new QuickPulseMetrics("\\ApplicationInsights\\Dependency Calls/Sec", counters.rdds, 1));
    metricsList.add(
        new QuickPulseMetrics(
            "\\ApplicationInsights\\Dependency Call Duration",
            (long) counters.rddsDuration,
            (int) counters.rdds));
    metricsList.add(
        new QuickPulseMetrics(
            "\\ApplicationInsights\\Dependency Calls Failed/Sec", counters.unsuccessfulRdds, 1));
    metricsList.add(
        new QuickPulseMetrics(
            "\\ApplicationInsights\\Dependency Calls Succeeded/Sec",
            counters.rdds - counters.unsuccessfulRdds,
            1));
    metricsList.add(
        new QuickPulseMetrics("\\ApplicationInsights\\Exceptions/Sec", counters.exceptions, 1));
    metricsList.add(
        new QuickPulseMetrics("\\Memory\\Committed Bytes", counters.memoryCommitted, 1));
    metricsList.add(
        new QuickPulseMetrics(
            "\\Processor(_Total)\\% Processor Time", (long) counters.cpuUsage, 1));

    return metricsList;
  }
}
