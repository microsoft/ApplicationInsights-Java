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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * Created by gupele on 6/4/2015.
 */
@SuppressWarnings("deprecation") // PoolingClientConnectionManager, DefaultHttpClient, HttpClient.getParams
final class ApacheSender42 implements ApacheSender {

    private final Object lock = new Object();
    private volatile PoolingClientConnectionManager cm;
    private volatile HttpClient httpClient;

    ApacheSender42() {
        InternalLogger.INSTANCE.info("Using Apache HttpClient 4.2");
    }

    private PoolingClientConnectionManager initializeConnectionManager() {
        PoolingClientConnectionManager pccm = new PoolingClientConnectionManager();
        pccm.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
        pccm.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
        return pccm;
    }

    private HttpClient initializeHttpClient(PoolingClientConnectionManager pccm) {
        HttpClient client = new DefaultHttpClient(pccm);

        HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params, REQUEST_TIMEOUT_IN_MILLIS);
        HttpConnectionParams.setSoTimeout(params, REQUEST_TIMEOUT_IN_MILLIS);
        return client;
    }

    @Override
    public HttpResponse sendPostRequest(HttpPost post) throws IOException {
        return httpClient.execute(post);
    }

    @Override
    public void dispose(HttpResponse response) {
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (cm != null) {
                cm.shutdown();
            }
        }
    }

    @Override
    public HttpClient getHttpClient() {
        HttpClient result = httpClient;
        if (result == null) {
            synchronized (lock) {
                result = httpClient;
                if (result == null) {
                    cm = initializeConnectionManager();
                    result = httpClient = initializeHttpClient(cm);
                }
            }
        }
        return result;
    }

    @Override
    public void enhanceRequest(HttpPost request) {
    }
}

