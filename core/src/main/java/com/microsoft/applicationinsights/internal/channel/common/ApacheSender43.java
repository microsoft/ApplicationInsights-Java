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

package com.microsoft.applicationinsights.internal.channel.common;

import java.io.IOException;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * Created by gupele on 6/4/2015.
 */
final class ApacheSender43 implements ApacheSender {
    private final Object lock = new Object();
    private volatile CloseableHttpClient httpClient;
    private volatile PoolingHttpClientConnectionManager connectionManager;

    ApacheSender43() {
        InternalLogger.INSTANCE.info("Using Apache HttpClient 4.3");
    }

    @Override
    public HttpResponse sendPostRequest(HttpPost post) throws IOException {
        return getHttpClient().execute(post);
    }

    @Override
    public void dispose(HttpResponse response) {
        try {
            if (response != null) {
                ((CloseableHttpResponse)response).close();
            }
        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to send or failed to close response, exception: %s", e.toString());
        }
    }

    @Override
    public void close() {
        try {
            ((CloseableHttpClient)getHttpClient()).close();
        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to close http httpClient, exception: %s", e.toString());
        } finally {
            synchronized (lock) {
                if (connectionManager != null) {
                    connectionManager.shutdown();
                }
            }
        }
    }

    @Override
    public HttpClient getHttpClient() {
        CloseableHttpClient result = httpClient;
        if (result == null) {
            synchronized (lock) {
                connectionManager = initializeConnectionManager();
                result = httpClient;
                if (result == null) {
                    httpClient = result = initializeClient(connectionManager);
                }
            }
        }
        return result;
    }

    private static PoolingHttpClientConnectionManager initializeConnectionManager() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
        return cm;
    }

    private static CloseableHttpClient initializeClient(HttpClientConnectionManager cm) {
        return HttpClients.custom()
                .setConnectionManager(cm)
                .setConnectionManagerShared(true)
                .useSystemProperties()
                .build();
    }

    @Override
    public void enhanceRequest(HttpPost request) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(REQUEST_TIMEOUT_IN_MILLIS)
                .setSocketTimeout(REQUEST_TIMEOUT_IN_MILLIS)
                .setConnectTimeout(REQUEST_TIMEOUT_IN_MILLIS)
                .setSocketTimeout(REQUEST_TIMEOUT_IN_MILLIS).build();

        request.setConfig(requestConfig);
    }
}
