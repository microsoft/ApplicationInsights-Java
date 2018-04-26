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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

public class CdsProfileFetcher implements AppProfileFetcher {

	private CloseableHttpAsyncClient httpClient;
    private String endpointAddress;
    private static final String ProfileQueryEndpointAppIdFormat = "%s/api/profiles/%s/appId";
    private static final String DefaultProfileQueryEndpointAddress = "https://dc.services.visualstudio.com";
	private static final int MAX_RETRIES = 3;

    // cache of tasks per ikey
    private final ConcurrentMap<String, Future<HttpResponse>> tasks;
    
    // failure counters per ikey
    private final Map<String, Integer> failureCounters;

    public CdsProfileFetcher() {
        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(5000)
            .setConnectTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .build();

        setHttpClient(HttpAsyncClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .useSystemProperties()
            .build());
        
        this.httpClient.start();

        this.tasks = new ConcurrentHashMap<String, Future<HttpResponse>>();
        this.failureCounters = new HashMap<String, Integer>();

        this.endpointAddress = DefaultProfileQueryEndpointAddress;
    }

	@Override
	public ProfileFetcherResult fetchAppProfile(String instrumentationKey) throws InterruptedException, ExecutionException, ParseException, IOException {

        if (instrumentationKey == null || instrumentationKey.isEmpty()) {
            throw new IllegalArgumentException("instrumentationKey must be not null or empty");
        }

        ProfileFetcherResult result = new ProfileFetcherResult(null, ProfileFetcherResultTaskStatus.PENDING);

        // check if we have tried resolving this ikey too many times. If so, quit to save on perf.
        Integer failureCounter = this.failureCounters.get(instrumentationKey);
        if (failureCounter != null && failureCounter.intValue() >= MAX_RETRIES) {
            InternalLogger.INSTANCE.warn("The profile fetch task will not execute. Max number of retries reached.");
            return result;
        }

        Future<HttpResponse> currentTask = this.tasks.get(instrumentationKey);

        // if no task currently exists for this ikey, then let's create one.
        if (currentTask == null) {
            currentTask = createFetchTask(instrumentationKey);
            this.tasks.putIfAbsent(instrumentationKey, currentTask);
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

	public void setHttpClient(CloseableHttpAsyncClient client) {
        this.httpClient = client;
        SDKShutdownActivity.INSTANCE.register(this.httpClient);
    }

    public void setEndpointAddress(String endpoint) throws MalformedURLException {
        // set endpoint address to the base address (e.g. https://dc.services.visualstudio.com)
        // later we will append the profile/ikey segment
        URL url = new URL(endpoint);
        String urlStr = url.toString();
        this.endpointAddress = urlStr.substring(0, urlStr.length() - url.getFile().length());
    }

    private Future<HttpResponse> createFetchTask(String instrumentationKey) {
		HttpGet request = new HttpGet(String.format(ProfileQueryEndpointAppIdFormat, this.endpointAddress, instrumentationKey));
        return this.httpClient.execute(request, null);
    }

    private synchronized void incrementFailureCount(String instrumentationKey) {
        Integer failureCounter = this.failureCounters.get(instrumentationKey);
        if (failureCounter == null) {
            this.failureCounters.put(instrumentationKey, new Integer(1));
        } else {
            this.failureCounters.put(instrumentationKey, new Integer(failureCounter.intValue() + 1));
        }
    }

	@Override
	public void close() throws IOException {
        this.httpClient.close();
	}
}