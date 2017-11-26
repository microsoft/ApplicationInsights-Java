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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.util.EntityUtils;

public class CdsProfileFetcher implements AppProfileFetcher {

	private HttpAsyncClient httpClient;
    private String endpointAddress;
    private static final String ProfileQueryEndpointAppIdFormat = "%s/api/profiles/%s/appId";
    private static final String DefaultProfileQueryEndpointAddress = "https://dc.services.visualstudio.com";

    // cache of tasks per ikey
    private final ConcurrentHashMap<String, Future<HttpResponse>> tasks;

    public CdsProfileFetcher() {
        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(3000)
            .setConnectTimeout(3000).build();

        this.httpClient = HttpAsyncClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .build();
        
        ((CloseableHttpAsyncClient)this.httpClient).start();

        this.tasks = new ConcurrentHashMap<String, Future<HttpResponse>>();
        this.endpointAddress = DefaultProfileQueryEndpointAddress;
    }

	@Override
	public ProfileFetcherResult fetchAppProfile(String instrumentationKey) throws InterruptedException, ExecutionException, ParseException, IOException {

        if (instrumentationKey == null || instrumentationKey.isEmpty()) {
            throw new IllegalArgumentException("instrumentationKey must be not null or empty");
        }

        ProfileFetcherResult result = new ProfileFetcherResult(null, ProfileFetcherResultTaskStatus.PENDING);
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
                return new ProfileFetcherResult(null, ProfileFetcherResultTaskStatus.FAILED);
            }

            String appId = EntityUtils.toString(response.getEntity());
            return new ProfileFetcherResult(appId, ProfileFetcherResultTaskStatus.COMPLETE);

        } finally {
            // remove task as we're done with it.
            this.tasks.remove(instrumentationKey);
        }
    }

	public void setHttpClient(HttpAsyncClient client) {
        this.httpClient = client;
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
}