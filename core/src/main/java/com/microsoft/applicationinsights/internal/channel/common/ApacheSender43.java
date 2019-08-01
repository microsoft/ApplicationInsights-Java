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
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.microsoft.applicationinsights.internal.util.SSLOptionsUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

/**
 * Created by gupele on 6/4/2015.
 */
final class ApacheSender43 implements ApacheSender {

    private final AtomicReference<CloseableHttpClient> httpClientRef = new AtomicReference<>();

    static ApacheSender43 create() {
        final ApacheSender43 sender = new ApacheSender43();
        final String[] allowedProtocols = SSLOptionsUtil.getAllowedProtocols();
        Thread initThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
                                .register("https", new SSLConnectionSocketFactory(SSLContexts.createDefault(), allowedProtocols, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier()))
                                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                                .build());
                        cm.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
                        cm.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
                        sender.httpClientRef.compareAndSet(null, HttpClients.custom()
                                .setConnectionManager(cm)
                                .useSystemProperties()
                                .build());
                        synchronized (sender.httpClientRef) {
                            sender.httpClientRef.notifyAll();
                        }
                    }
                }, ApacheSender43.class.getSimpleName()+"_initializer");
        initThread.setDaemon(true);
        initThread.start();
        return sender;
    }

    private ApacheSender43() {}

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
            InternalLogger.INSTANCE.error("Failed to close http client, exception: %s", e.toString());
        }
    }

    @Override
    public HttpClient getHttpClient() {
        synchronized (httpClientRef) {
            try {
                while (httpClientRef.get() == null) {
                    httpClientRef.wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return httpClientRef.get();
    }

    @Override
    public void enhanceRequest(HttpPost request) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(REQUEST_TIMEOUT_IN_MILLIS)
                .setSocketTimeout(REQUEST_TIMEOUT_IN_MILLIS)
                .setConnectTimeout(REQUEST_TIMEOUT_IN_MILLIS)
                .build();

        request.setConfig(requestConfig);
    }
}
