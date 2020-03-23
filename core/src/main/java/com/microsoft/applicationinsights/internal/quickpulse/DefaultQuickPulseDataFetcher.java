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

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gupele on 12/12/2016.
 */
final class DefaultQuickPulseDataFetcher implements QuickPulseDataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultQuickPulseDataFetcher.class);

    private static final String QP_BASE_URI = "https://rt.services.visualstudio.com/QuickPulseService.svc";
    private final ArrayBlockingQueue<HttpPost> sendQueue;
    private final TelemetryConfiguration config;
    private final String ikey;
    private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
    private String postPrefix;
    private final String sdkVersion;

    public DefaultQuickPulseDataFetcher(ArrayBlockingQueue<HttpPost> sendQueue, TelemetryConfiguration config,
                                        String instanceName, String quickPulseId) {
        this(sendQueue, config, null, instanceName, quickPulseId);
    }

    @Deprecated
    public DefaultQuickPulseDataFetcher(final ArrayBlockingQueue<HttpPost> sendQueue, final String ikey, final String instanceName, final String quickPulseId) {
        this(sendQueue, null, ikey, instanceName, quickPulseId);
    }

    private DefaultQuickPulseDataFetcher(ArrayBlockingQueue<HttpPost> sendQueue, TelemetryConfiguration config, String ikey, String instanceName, String quickPulseId) {
        this.sendQueue = sendQueue;
        this.config = config;
        this.ikey = ikey;
        sdkVersion = getCurrentSdkVersion();
        final StringBuilder sb = new StringBuilder();
        sb.append("[{");
        formatDocuments(sb);
        sb.append("\"Instance\": \"").append(instanceName).append("\",");
        sb.append("\"InstrumentationKey\": \"").append(ikey).append("\",");
        sb.append("\"InvariantVersion\": 1,");
        sb.append("\"MachineName\": \"").append(instanceName).append("\",");
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
    public void prepareQuickPulseDataForSend() {
        try {
            QuickPulseDataCollector.FinalCounters counters = QuickPulseDataCollector.INSTANCE.getAndRestart();

            final Date currentDate = new Date();
            final HttpPost request = networkHelper.buildRequest(currentDate, getEndpointUrl());

            final ByteArrayEntity postEntity = buildPostEntity(counters);

            request.setEntity(postEntity);

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

    @VisibleForTesting
    String getEndpointUrl() {
        return getQuickPulseEndpoint() + "/post?ikey=" + getInstrumentationKey();
    }

    private String getQuickPulseEndpoint() {
         return config == null ? QP_BASE_URI : config.getEndpointProvider().getLiveEndpointURL().toString();
    }

    private String getInstrumentationKey() {
        if (config != null) {
            return config.getInstrumentationKey();
        } else {
            return ikey;
        }
    }

    private ByteArrayEntity buildPostEntity(QuickPulseDataCollector.FinalCounters counters) {
        StringBuilder sb = new StringBuilder(postPrefix);
        formatMetrics(counters, sb);
        sb.append("\"Timestamp\": \"\\/Date(");
        long ms = System.currentTimeMillis();
        sb.append(ms);
        sb.append(")\\/\",");
        sb.append("\"Version\": \"");
        sb.append(sdkVersion);
        sb.append("\"}]");
        return new ByteArrayEntity(sb.toString().getBytes());
    }

    private void formatDocuments(StringBuilder sb) {
        sb.append("\"Documents\": [] ,");
    }

    private void formatSingleMetric(StringBuilder sb, String metricName, double metricValue, int metricWeight, Boolean includeComma) {
        String comma = includeComma ? "," : "";
        sb.append(String.format("{\"Name\": \"%s\",\"Value\": %s,\"Weight\": %s}%s", metricName, metricValue, metricWeight, comma));
    }

    private void formatSingleMetric(StringBuilder sb, String metricName, long metricValue, int metricWeight, Boolean includeComma) {
        String comma = includeComma ? "," : "";
        sb.append(String.format("{\"Name\": \"%s\",\"Value\": %s,\"Weight\": %s}%s", metricName, metricValue, metricWeight, comma));
    }

    private void formatMetrics(QuickPulseDataCollector.FinalCounters counters, StringBuilder sb) {
        sb.append("\"Metrics\":[");
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Requests\\/Sec", counters.requests, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Request Duration", counters.requestsDuration, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Requests Failed\\/Sec", counters.unsuccessfulRequests, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Requests Succeeded\\/Sec", (counters.requests - counters.unsuccessfulRequests), 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Dependency Calls\\/Sec", counters.rdds, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Dependency Call Duration", counters.rddsDuration, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Dependency Calls Failed\\/Sec", counters.unsuccessfulRdds, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Dependency Calls Succeeded\\/Sec", counters.rdds - counters.unsuccessfulRdds, 1, true);
        formatSingleMetric(sb, "\\\\ApplicationInsights\\\\Exceptions\\/Sec", counters.exceptions, 1, true);
        formatSingleMetric(sb, "\\\\Memory\\\\Committed Bytes", counters.memoryCommitted, 1, true);
        formatSingleMetric(sb, "\\\\Processor(_Total)\\\\% Processor Time", counters.cpuUsage, 1, false);
        sb.append("],");
    }
}
