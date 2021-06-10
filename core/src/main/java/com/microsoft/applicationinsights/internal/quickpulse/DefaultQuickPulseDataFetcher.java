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

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

import com.azure.core.http.HttpRequest;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultQuickPulseDataFetcher implements QuickPulseDataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultQuickPulseDataFetcher.class);

    private static final String QP_BASE_URI = "https://rt.services.visualstudio.com/QuickPulseService.svc";
    private final ArrayBlockingQueue<HttpRequest> sendQueue;
    private final TelemetryClient telemetryClient;
    private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
    private final String postPrefix;
    private final String sdkVersion;

    public DefaultQuickPulseDataFetcher(ArrayBlockingQueue<HttpRequest> sendQueue, TelemetryClient telemetryClient, String machineName,
                                        String instanceName, String quickPulseId) {
        this.sendQueue = sendQueue;
        this.telemetryClient = telemetryClient;
        sdkVersion = getCurrentSdkVersion();
        StringBuilder sb = new StringBuilder();

        // FIXME (trask) what about azure functions consumption plan where role name not available yet?
        String roleName = telemetryClient.getRoleName();

        if (!LocalStringsUtils.isNullOrEmpty(roleName)) {
            roleName = "\"" + roleName + "\"";
        }

        sb.append("[{");
        formatDocuments(sb);
        sb.append("\"Instance\": \"").append(instanceName).append("\",");
        // FIXME (trask) this seemed to be working when it was always null ikey here??
        sb.append("\"InstrumentationKey\": \"").append(telemetryClient.getInstrumentationKey()).append("\",");
        sb.append("\"InvariantVersion\": ").append(QuickPulse.QP_INVARIANT_VERSION).append(",");
        sb.append("\"MachineName\": \"").append(machineName).append("\",");
        sb.append("\"RoleName\": ").append(roleName).append(",");
        sb.append("\"StreamId\": \"").append(quickPulseId).append("\",");
        postPrefix = sb.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("{} using endpoint {}", DefaultQuickPulseDataFetcher.class.getSimpleName(), getQuickPulseEndpoint());
        }
    }

    /**
     * Get SDK Version from properties
     * @return current SDK version
     */
     /* Visible for testing */ String getCurrentSdkVersion() {
        return PropertyHelper.getQualifiedSdkVersionString();
    }

    @Override
    public void prepareQuickPulseDataForSend(String redirectedEndpoint) {
        try {
            QuickPulseDataCollector.FinalCounters counters = QuickPulseDataCollector.INSTANCE.getAndRestart();

            Date currentDate = new Date();
            String endpointPrefix = LocalStringsUtils.isNullOrEmpty(redirectedEndpoint) ? getQuickPulseEndpoint() : redirectedEndpoint;
            HttpRequest request = networkHelper.buildRequest(currentDate, this.getEndpointUrl(endpointPrefix));
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
         return telemetryClient == null ? QP_BASE_URI : telemetryClient.getEndpointProvider().getLiveEndpointUrl().toString();
    }

    private String getInstrumentationKey() {
        return telemetryClient.getInstrumentationKey();
    }

    private String buildPostEntity(QuickPulseDataCollector.FinalCounters counters) {
        StringBuilder sb = new StringBuilder(postPrefix);
        formatMetrics(counters, sb);
        sb.append("\"Timestamp\": \"\\/Date(");
        long ms = System.currentTimeMillis();
        sb.append(ms);
        sb.append(")\\/\",");
        sb.append("\"Version\": \"");
        sb.append(sdkVersion);
        sb.append("\"}]");
        return sb.toString();
    }

    private static void formatDocuments(StringBuilder sb) {
        sb.append("\"Documents\": [] ,");
    }

    private static void formatSingleMetric(StringBuilder sb, String metricName, double metricValue, int metricWeight, boolean includeComma) {
        String comma = includeComma ? "," : "";
        sb.append(String.format("{\"Name\": \"%s\",\"Value\": %s,\"Weight\": %s}%s", metricName, metricValue, metricWeight, comma));
    }

    private static void formatSingleMetric(StringBuilder sb, String metricName, long metricValue, int metricWeight, boolean includeComma) {
        String comma = includeComma ? "," : "";
        sb.append(String.format("{\"Name\": \"%s\",\"Value\": %s,\"Weight\": %s}%s", metricName, metricValue, metricWeight, comma));
    }

    private static void formatMetrics(QuickPulseDataCollector.FinalCounters counters, StringBuilder sb) {
        sb.append("\"Metrics\":[");
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Requests\\/Sec", counters.requests, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Request Duration", counters.requestsDuration, (int)counters.requests, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Requests Failed\\/Sec", counters.unsuccessfulRequests, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Requests Succeeded\\/Sec", (counters.requests - counters.unsuccessfulRequests), 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Dependency Calls\\/Sec", counters.rdds, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Dependency Call Duration", counters.rddsDuration, (int)counters.rdds, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Dependency Calls Failed\\/Sec", counters.unsuccessfulRdds, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Dependency Calls Succeeded\\/Sec", counters.rdds - counters.unsuccessfulRdds, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Exceptions\\/Sec", counters.exceptions, 1, true);
        formatSingleMetric(sb, "\\\\Memory\\\\Committed Bytes", counters.memoryCommitted, 1, true);
        formatSingleMetric(sb, "\\\\Processor(_Total)\\\\% Processor Time", counters.cpuUsage, 1, false);
        sb.append("],");
    }
}
