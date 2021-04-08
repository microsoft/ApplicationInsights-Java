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

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.channel.common.LazyHttpClient;
import com.microsoft.applicationinsights.internal.util.ExceptionStats;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

public class AppIdSupplier implements AiAppId.Supplier {

    private static final Logger logger = LoggerFactory.getLogger(AppIdSupplier.class);

    public static final AppIdSupplier INSTANCE = new AppIdSupplier();

    // note: app id is used by distributed trace headers and (soon) jfr profiling
    public static void registerAndStartAppIdRetrieval() {
        AiAppId.setSupplier(INSTANCE);
        startAppIdRetrieval();
    }

    public static void startAppIdRetrieval() {
        INSTANCE.internalStartAppIdRetrieval();
    }

    private final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(AppIdSupplier.class));

    private final ExceptionStats exceptionStats = new ExceptionStats(GetAppIdTask.class, "unable to retrieve appId");

    // guarded by taskLock
    private GetAppIdTask task;
    private final Object taskLock = new Object();

    private volatile String appId;

    private void internalStartAppIdRetrieval() {
        TelemetryConfiguration configuration = TelemetryConfiguration.getActive();
        String instrumentationKey = configuration.getInstrumentationKey();
        GetAppIdTask newTask = new GetAppIdTask(configuration.getEndpointProvider().getAppIdEndpointURL(instrumentationKey));
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
        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
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

        private final URI uri;

        // 1, 2, 4, 8, 16, 32, 60 (max)
        private volatile long backoffSeconds = 1;

        private volatile boolean cancelled;

        private GetAppIdTask(URI uri) {
            this.uri = uri;
        }

        @Override
        public void run() {
            if (cancelled) {
                return;
            }

            HttpGet request = new HttpGet(uri);

            HttpResponse response;
            try {
                response = LazyHttpClient.getInstance().execute(request);
            } catch (Exception e) {
                // TODO handle Friendly SSL exception
                logger.debug(e.getMessage(), e);
                backOff("exception sending request to " + uri, e);
                return;
            }

            String body;
            try {
                body = EntityUtils.toString(response.getEntity());
            } catch (IOException e) {
                logger.debug(e.getMessage(), e);
                backOff("exception reading response from " + uri, e);
                return;
            }

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                backOff("received " + statusCode + " " + response.getStatusLine().getReasonPhrase() + " from " + uri
                        + "\nfull response:\n" + body, null);
                return;
            }

            // check for case when breeze returns invalid value
            if (body == null || body.isEmpty()) {
                backOff("received empty body from " + uri, null);
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
