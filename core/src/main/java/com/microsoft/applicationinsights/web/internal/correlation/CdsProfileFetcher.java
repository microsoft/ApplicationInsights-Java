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

package com.microsoft.applicationinsights.web.internal.correlation;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;
import com.microsoft.applicationinsights.internal.util.PeriodicTaskPool;
import com.microsoft.applicationinsights.internal.util.SSLOptionsUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CdsProfileFetcher implements ApplicationIdResolver, Closeable {

    private CloseableHttpAsyncClient httpClient;
    private String endpointAddress = null;
    private static final String PROFILE_QUERY_ENDPOINT_APP_ID_FORMAT = "%s/api/profiles/%s/appId";

    // cache of tasks per ikey
    /* Visible for Testing */ final ConcurrentMap<String, Future<HttpResponse>> tasks;

    // failure counters per ikey
    /* Visible for Testing */ final ConcurrentMap<String, Integer> failureCounters;

    private final PeriodicTaskPool taskThreadPool;
    private final CdsRetryPolicy retryPolicy;

    public CdsProfileFetcher() {
        this(new CdsRetryPolicy());
    }

    public CdsProfileFetcher(CdsRetryPolicy retryPolicy) {
        taskThreadPool = new PeriodicTaskPool(1, CdsProfileFetcher.class.getSimpleName());
        this.retryPolicy = retryPolicy;

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .build();

        final String[] allowedProtocols = SSLOptionsUtil.getAllowedProtocols();
        setHttpClient(HttpAsyncClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setSSLStrategy(new SSLIOSessionStrategy(SSLContexts.createDefault(), allowedProtocols, null, SSLIOSessionStrategy.getDefaultHostnameVerifier()))
                .useSystemProperties()
                .build());

        long resetInterval = retryPolicy.getResetPeriodInMinutes();
        PeriodicTaskPool.PeriodicRunnableTask cdsRetryClearTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new CachePurgingRunnable(),
                resetInterval, resetInterval, TimeUnit.MINUTES, "cdsRetryClearTask");

        this.tasks = new ConcurrentHashMap<>();
        this.failureCounters = new ConcurrentHashMap<>();

        taskThreadPool.executePeriodicRunnableTask(cdsRetryClearTask);
        this.httpClient.start();

        SDKShutdownActivity.INSTANCE.register(this);
    }

    @Override
    public ProfileFetcherResult fetchApplicationId(String instrumentationKey, TelemetryConfiguration configuration) throws ApplicationIdResolutionException, InterruptedException {
        try {
            return internalFetchAppProfile(instrumentationKey, configuration);
        } catch (ExecutionException | IOException e) {
            throw new ApplicationIdResolutionException(e);
        }
    }

    private ProfileFetcherResult internalFetchAppProfile(String instrumentationKey, TelemetryConfiguration configuration) throws InterruptedException, ExecutionException, IOException {
        if (StringUtils.isEmpty(instrumentationKey)) {
            throw new IllegalArgumentException("instrumentationKey must not be null or empty");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("configuration must not be null");
        }

        ProfileFetcherResult result = new ProfileFetcherResult(null, ProfileFetcherResultTaskStatus.PENDING);

        // check if we have tried resolving this ikey too many times. If so, quit to save on perf.
        if (failureCounters.containsKey(instrumentationKey) && failureCounters.get(instrumentationKey) >= retryPolicy.getMaxInstantRetries()) {
            InternalLogger.INSTANCE.info("The profile fetch task will not execute for next %d minutes. Max number of retries reached.", retryPolicy.getResetPeriodInMinutes());
            return result;
        }

        Future<HttpResponse> currentTask = this.tasks.get(instrumentationKey);

        // if no task currently exists for this ikey, then let's create one.
        if (currentTask == null) {
            currentTask = createFetchTask(instrumentationKey, configuration);
            Future<HttpResponse> mapValue = this.tasks.putIfAbsent(instrumentationKey, currentTask);
            // it's possible for another thread to create a fetch task
            if (mapValue != null) { // if there is already a mapping, then let's discard the new one and check if the existing task has completed.
                currentTask = mapValue;
            }
        }

        // check if task is still pending
        if (!currentTask.isDone()) {
            return result;
        }

        // task is ready, we can call get() now.
        try {
            HttpResponse response = currentTask.get();

            if (response.getStatusLine().getStatusCode() != 200) {
                incrementFailureCount(instrumentationKey);
                return new ProfileFetcherResult(null, ProfileFetcherResultTaskStatus.FAILED);
            }

            String appId = EntityUtils.toString(response.getEntity());

            //check for case when breeze returns invalid value
            if (appId == null || appId.isEmpty()) {
                incrementFailureCount(instrumentationKey);
                return new ProfileFetcherResult(null, ProfileFetcherResultTaskStatus.FAILED);
            }

            return new ProfileFetcherResult(appId, ProfileFetcherResultTaskStatus.COMPLETE);

        } catch (Exception ex) {
            incrementFailureCount(instrumentationKey);
            throw ex;
        } finally {
            // remove task as we're done with it.
            this.tasks.remove(instrumentationKey);
        }
    }

    void setHttpClient(CloseableHttpAsyncClient client) {
        this.httpClient = client;
    }

    /**
     * @deprecated Set endpoints using {@link TelemetryConfiguration#setConnectionString(String)}.
     */
    @Deprecated
    public void setEndpointAddress(String endpoint) throws MalformedURLException {
        // set endpoint address to the base address (e.g. https://dc.services.visualstudio.com)
        // later we will append the profile/ikey segment
        URL url = new URL(endpoint);
        String urlStr = url.toString();
        this.endpointAddress = urlStr.substring(0, urlStr.length() - url.getFile().length());
        if (InternalLogger.INSTANCE.isTraceEnabled()) {
            InternalLogger.INSTANCE.trace("%s endpoint override: %s", CdsProfileFetcher.class.getSimpleName(), this.endpointAddress);
        }
    }

    private Future<HttpResponse> createFetchTask(String instrumentationKey, TelemetryConfiguration configuration) {
        final HttpGet request;
        if (endpointAddress == null) {
            request = new HttpGet(configuration.getEndpointProvider().getAppIdEndpointURL(instrumentationKey));
        } else {
            request = new HttpGet(String.format(PROFILE_QUERY_ENDPOINT_APP_ID_FORMAT, this.endpointAddress, instrumentationKey));
        }

        return this.httpClient.execute(request, null);
    }

    private void incrementFailureCount(String instrumentationKey) {
        if (!this.failureCounters.containsKey(instrumentationKey)) {
            this.failureCounters.put(instrumentationKey, 0);
        }
        this.failureCounters.put(instrumentationKey, this.failureCounters.get(instrumentationKey) + 1);
    }

    @Override
    public void close() throws IOException {
        this.httpClient.close();
        this.taskThreadPool.stop(5, TimeUnit.SECONDS);
    }

    /**
     * Runnable that is used to clear the retry counters and pending unresolved tasks.
     */
    private class CachePurgingRunnable implements Runnable {
        @Override
        public void run() {
            tasks.clear();
            failureCounters.clear();
        }
    }

    /**
     * Responsible for CDS Retry Policy configuration.
     */
    public static class CdsRetryPolicy {

        public static final int DEFAULT_MAX_INSTANT_RETRIES = 3;
        public static final int DEFAULT_RESET_PERIOD_IN_MINUTES = 240;
        /**
         * Maximum number of instant retries to CDS to resolve ikey to AppId.
         */
        private int maxInstantRetries;

        /**
         * The interval in minutes for retry counters and pending tasks to be cleaned.
         */
        private long resetPeriodInMinutes;

        public int getMaxInstantRetries() {
            return maxInstantRetries;
        }

        public long getResetPeriodInMinutes() {
            return resetPeriodInMinutes;
        }

        public void setMaxInstantRetries(int maxInstantRetries) {
            if (maxInstantRetries < 1) {
                throw new IllegalArgumentException("CDS maxInstantRetries should be at least 1");
            }
            this.maxInstantRetries = maxInstantRetries;
        }

        public void setResetPeriodInMinutes(long resetPeriodInMinutes) {
            if (resetPeriodInMinutes < 1) {
                throw new IllegalArgumentException("CDS retries reset interval should be at least 1 minute");
            }
            this.resetPeriodInMinutes = resetPeriodInMinutes;
        }

        public CdsRetryPolicy() {
            maxInstantRetries = DEFAULT_MAX_INSTANT_RETRIES;
            resetPeriodInMinutes = DEFAULT_RESET_PERIOD_IN_MINUTES;
        }
    }
}
