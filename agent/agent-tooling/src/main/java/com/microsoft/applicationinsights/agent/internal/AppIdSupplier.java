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

package com.microsoft.applicationinsights.agent.internal;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.channel.common.LazyAzureHttpClient;
import com.microsoft.applicationinsights.internal.util.ExceptionStats;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

// note: app id is used by distributed trace headers and (soon) jfr profiling
public class AppIdSupplier implements AiAppId.Supplier {

    private static final Logger logger = LoggerFactory.getLogger(AppIdSupplier.class);

    public static final AppIdSupplier INSTANCE = new AppIdSupplier();

    private final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(AppIdSupplier.class));

    private final ExceptionStats exceptionStats = new ExceptionStats(GetAppIdTask.class, "unable to retrieve appId");

    // guarded by taskLock
    private GetAppIdTask task;
    private final Object taskLock = new Object();

    private volatile String appId;

    public void registerAndStartAppIdRetrieval() {
        AiAppId.setSupplier(this);
        startAppIdRetrieval();
    }

    public void startAppIdRetrieval() {
        TelemetryClient telemetryClient = TelemetryClient.getActive();
        String instrumentationKey = telemetryClient.getInstrumentationKey();
        GetAppIdTask newTask = new GetAppIdTask(telemetryClient.getEndpointProvider().getAppIdEndpointUrl(instrumentationKey));
        synchronized (taskLock) {
            appId = null;
            if (task != null) {
                // in case prior task is still running (can be called multiple times from JsonConfigPolling)
                task.cancelled = true;
            }
            task = newTask;
        }
        scheduledExecutor.submit(newTask);
    }

    public String get() {
        String instrumentationKey = TelemetryClient.getActive().getInstrumentationKey();
        if (instrumentationKey == null) {
            // this is possible in Azure Function consumption plan prior to "specialization"
            return "";
        }

        //it's possible the appId returned is null (e.g. async task is still pending or has failed). In this case, just
        //return and let the next request resolve the ikey.
        if (appId == null) {
            logger.debug("appId has not been retrieved yet (e.g. task may be pending or failed)");
            return "";
        }
        return appId;
    }

    private class GetAppIdTask implements Runnable {

        private final URL url;

        // 1, 2, 4, 8, 16, 32, 60 (max)
        private volatile long backoffSeconds = 1;

        private volatile boolean cancelled;

        private GetAppIdTask(URL url) {
            this.url = url;
        }

        @Override
        public void run() {
            if (cancelled) {
                return;
            }

            HttpRequest request = new HttpRequest(HttpMethod.GET, url);
            HttpResponse response;
            try {
                response = LazyAzureHttpClient.getInstance().send(request).block();
            } catch (Exception e) {
                // TODO handle Friendly SSL exception
                logger.debug(e.getMessage(), e);
                backOff("exception sending request to " + url, e);
                return;
            }

            // this check is needed to make spotbugs happy
            if (response == null) {
                throw new IllegalStateException("response should never be null");
            }

            String body = response.getBodyAsString().block();
            int statusCode = response.getStatusCode();
            if (statusCode != 200) {
                backOff("received " + statusCode + " from " + url
                        + "\nfull response:\n" + body, null);
                return;
            }
            
            // check for case when breeze returns invalid value
            if (body == null || body.isEmpty()) {
                backOff("received empty body from " + url, null);
                return;
            }

            logger.debug("appId retrieved: {}", body);
            appId = body;
        }

        private void backOff(String warningMessage, Exception exception) {
            exceptionStats.recordFailure(warningMessage, exception);
            scheduledExecutor.schedule(this, backoffSeconds, SECONDS);
            backoffSeconds = Math.min(backoffSeconds * 2, 60);
        }
    }
}
