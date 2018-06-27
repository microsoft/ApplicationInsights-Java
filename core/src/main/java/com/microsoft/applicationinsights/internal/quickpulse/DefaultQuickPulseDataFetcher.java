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

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

/** Created by gupele on 12/12/2016. */
final class DefaultQuickPulseDataFetcher implements QuickPulseDataFetcher {
  private static final String QP_BASE_URI =
      "https://rt.services.visualstudio.com/QuickPulseService.svc/";
  private final String quickPulsePostUri;
  private final ArrayBlockingQueue<HttpPost> sendQueue;
  private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
  private String postPrefix;

  public DefaultQuickPulseDataFetcher(
      final ArrayBlockingQueue<HttpPost> sendQueue,
      final String ikey,
      final String instanceName,
      final String quickPulseId) {
    quickPulsePostUri = QP_BASE_URI + "post?ikey=" + ikey;
    this.sendQueue = sendQueue;
    final StringBuilder sb = new StringBuilder();
    sb.append("[{");
    formatDocuments(sb);
    sb.append("\"Instance\": \"" + instanceName + "\",");
    sb.append("\"InstrumentationKey\": \"" + ikey + "\",");
    sb.append("\"InvariantVersion\": 2,");
    sb.append("\"MachineName\": \"" + instanceName + "\",");
    sb.append("\"StreamId\": \"" + quickPulseId + "\",");

    postPrefix = sb.toString();
  }

  @Override
  public void prepareQuickPulseDataForSend() {
    try {
      QuickPulseDataCollector.FinalCounters counters =
          QuickPulseDataCollector.INSTANCE.getAndRestart();

      final Date currentDate = new Date();
      final HttpPost request = networkHelper.buildRequest(currentDate, quickPulsePostUri);

      final ByteArrayEntity postEntity = buildPostEntity(counters);

      request.setEntity(postEntity);

      if (!sendQueue.offer(request)) {
        InternalLogger.INSTANCE.trace("Quick Pulse send queue is full");
      }
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable e) {
      try {
        InternalLogger.INSTANCE.trace("Quick Pulse failed to prepare data for send");
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t2) {
        // chomp
      }
    }
  }

  private ByteArrayEntity buildPostEntity(QuickPulseDataCollector.FinalCounters counters) {
    StringBuilder sb = new StringBuilder(postPrefix);
    formatMetrics(counters, sb);
    sb.append("\"Timestamp\": \"\\/Date(");
    long ms = System.currentTimeMillis();
    sb.append(ms);
    sb.append(")\\/\",");
    sb.append("\"Version\": \"2.2.0-738\"");
    sb.append("}]");
    ByteArrayEntity bae = new ByteArrayEntity(sb.toString().getBytes());
    return bae;
  }

  private void formatDocuments(StringBuilder sb) {
    sb.append("\"Documents\": [] ,");
  }

  private void formatSingleMetric(
      StringBuilder sb,
      String metricName,
      double metricValue,
      int metricWeight,
      Boolean includeComma) {
    String comma = includeComma ? "," : "";
    sb.append(
        String.format(
            "{\"Name\": \"%s\",\"Value\": %s,\"Weight\": %s}%s",
            metricName, metricValue, metricWeight, comma));
  }

  private void formatSingleMetric(
      StringBuilder sb,
      String metricName,
      long metricValue,
      int metricWeight,
      Boolean includeComma) {
    String comma = includeComma ? "," : "";
    sb.append(
        String.format(
            "{\"Name\": \"%s\",\"Value\": %s,\"Weight\": %s}%s",
            metricName, metricValue, metricWeight, comma));
  }

  private void formatSingleMetric(
      StringBuilder sb,
      String metricName,
      int metricValue,
      int metricWeight,
      Boolean includeComma) {
    String comma = includeComma ? "," : "";
    sb.append(
        String.format(
            "{\"Name\": \"%s\",\"Value\": %s,\"Weight\": %s}%s",
            metricName, metricValue, metricWeight, comma));
  }

  private void formatMetrics(QuickPulseDataCollector.FinalCounters counters, StringBuilder sb) {
    sb.append("\"Metrics\":[");
    formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Requests\\/Sec", counters.requests, 1, true);
    formatSingleMetric(
        sb, "\\\\ApplicationInsights\\\\Request Duration", counters.requestsDuration, 1, true);
    formatSingleMetric(
        sb,
        "\\\\ApplicationInsights\\\\Requests Failed\\/Sec",
        counters.unsuccessfulRequests,
        1,
        true);
    formatSingleMetric(
        sb,
        "\\\\ApplicationInsights\\\\Requests Succeeded\\/Sec",
        (counters.requests - counters.unsuccessfulRequests),
        1,
        true);
    formatSingleMetric(
        sb, "\\\\ApplicationInsights\\\\Dependency Calls\\/Sec", counters.rdds, 1, true);
    formatSingleMetric(
        sb, "\\\\ApplicationInsights\\\\Dependency Call Duration", counters.rddsDuration, 1, true);
    formatSingleMetric(
        sb,
        "\\\\ApplicationInsights\\\\Dependency Calls Failed\\/Sec",
        counters.unsuccessfulRdds,
        1,
        true);
    formatSingleMetric(
        sb,
        "\\\\ApplicationInsights\\\\Dependency Calls Succeeded\\/Sec",
        counters.rdds - counters.unsuccessfulRdds,
        1,
        true);
    formatSingleMetric(
        sb, "\\\\ApplicationInsights\\\\Exceptions\\/Sec", counters.exceptions, 1, true);
    formatSingleMetric(sb, "\\\\Memory\\\\Committed Bytes", counters.memoryCommitted, 1, true);
    formatSingleMetric(
        sb, "\\\\Processor(_Total)\\\\% Processor Time", counters.cpuUsage, 1, false);
    sb.append("],");
  }
}
