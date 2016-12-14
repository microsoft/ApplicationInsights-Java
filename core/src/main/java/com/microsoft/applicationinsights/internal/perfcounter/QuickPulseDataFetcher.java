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

package com.microsoft.applicationinsights.internal.perfcounter;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.apache.commons.lang3.text.StrBuilder;

/**
 * Created by gupele on 12/12/2016.
 */
final class QuickPulseDataFetcher {
    private final static String QP_BASE_URI = "https://rt.services.visualstudio.com/QuickPulseService.svc/ping?ikey=";
    private final String quickPulsePostUri;
    private final ArrayBlockingQueue<HttpPost> sendQueue;
    private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
    private String postPrefix;

    public QuickPulseDataFetcher(final ArrayBlockingQueue<HttpPost> sendQueue, final String ikey, final String instanceName, final String quickPulseId) {
        quickPulsePostUri = QP_BASE_URI + "post?ikey=" + ikey;
        this.sendQueue = sendQueue;
        final StrBuilder sb = new StrBuilder();
        sb.append("\"Instance\":\"" + instanceName + "\"," + "\"InstrumentationKey\":");
        sb.append(ikey);
        sb.append(",\"InvariantVersion\":2,\"MachineName\":\"");
        sb.append(instanceName);
        sb.append("\"");
        sb.append(",\"Version\":\"2.2.0-424\"");
        sb.append(",\"StreamId\":");
        sb.append(quickPulseId);

        postPrefix = sb.toString();
    }

    public void prepareQuickPulseDataForSend() {
        try {
            QuickPulseDataCollector.FinalCounters counters = QuickPulseDataCollector.INSTANCE.getAndRestart();

            final Date currentDate = new Date();
            HttpPost request = networkHelper.buildRequest(currentDate, quickPulsePostUri);

            final ByteArrayEntity postEntity = buildPostEntity(counters);

            request.setEntity(postEntity);

            if (!sendQueue.offer(request)) {
                InternalLogger.INSTANCE.trace("Quick Pulse send queue is full");
            }
        } catch (Throwable e) {
            InternalLogger.INSTANCE.trace("Quick Pulse failed to prepare data for send");
        }
    }

    private ByteArrayEntity buildPostEntity(QuickPulseDataCollector.FinalCounters counters) {
        StrBuilder sb = new StrBuilder(postPrefix);

        formatDocuments(sb);
        formatMetrics(counters, sb);

        sb.append(",\"Timestamp\": \"\\/Date(");

        long ms = System.currentTimeMillis();

        sb.append(ms);
        sb.append(")\\\\/\\\"\"");

        ByteArrayEntity bae = new ByteArrayEntity(sb.toString().getBytes());
        return bae;
    }

    private void formatDocuments(StrBuilder sb) {
        sb.append(",\"Documents\":null");
    }

    private void formatMetrics(QuickPulseDataCollector.FinalCounters counters, StrBuilder sb) {
        sb.append(
                String.format(",\"Metrics\":[{" +
                                "{\"Name\":\"\\\\ApplicationInsights\\\\Exceptions\",\"Value\": %s,\"Weight\":1}," +
                                "{\"Name\":\"\\\\Memory\\\\Committed Bytes\",\"Value\": %s,\"Weight\":1}," +
                                "{\"Name\":\"\\\\Memory\\\\Processor(_Total)\\\\%% Processor Time\",\"Value\": %s,\"Weight\":1}," +
                                "{\"Name\":\"\\\\ApplicationInsights\\\\Request\",\"Value\": %s,\"Weight\":1}," +
                                "{\"Name\":\"\\\\ApplicationInsights\\\\Request Duration\\/Sec\",\"Value\":%s,\"Weight\":%s}," +
                                "{\"Name\":\"\\\\ApplicationInsights\\\\Requests Failed\\/Sec\",\"Value\":%s,\"Weight\":1}," +
                                "{\"Name\":\"\\\\ApplicationInsights\\\\Dependency Calls\",\"Value\":%s,\"Weight\":1}," +
                                "{\"Name\":\"\\\\ApplicationInsights\\\\Dependency Calls Failed\\/Sec\",\"Value\":%s,\"Weight\":1}," +
                                "{\"Name\":\"\\\\ApplicationInsights\\\\Dependency Call Duration\\/Sec\",\"Value\":%s,\"Weight\":%s}}]",
                        counters.exceptions,
                        counters.memoryCommitted,
                        counters.cpuUsage,
                        counters.requests,
                        counters.requestsDuration, counters.requests,
                        counters.unsuccessfulRequests,
                        counters.rdds,
                        counters.unsuccessfulRdds,
                        counters.rddsDuration, counters.rdds));
    }
}
