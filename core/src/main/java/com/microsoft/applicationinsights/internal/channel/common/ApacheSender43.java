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
import java.util.Map;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.microsoft.applicationinsights.internal.util.Constants;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * Created by gupele on 6/4/2015.
 */
final class ApacheSender43 implements ApacheSender {

    private final CloseableHttpClient httpClient;

    public ApacheSender43(Map<String, String> keysAndValues) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);

        CredentialsProvider credentialsProvider = getCredentialsProvider(keysAndValues);

        HttpClientBuilder builder = HttpClients.custom();
        if (credentialsProvider != null) {
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }

        httpClient = builder
                .setConnectionManager(cm)
                .useSystemProperties()
                .build();
         }

    @Override
    public HttpResponse sendPostRequest(HttpPost post) throws IOException {
        return httpClient.execute(post);
    }

    @Override
    public void dispose(HttpResponse response) {
        try {
            if (response != null) {
                ((CloseableHttpResponse)response).close();
            }
        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to send or failed to close response, exception: %s", e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to close http client, exception: %s", e.getMessage());
        }
    }

    @Override
    public HttpClient getHttpClient() {
        return httpClient;
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

    private CredentialsProvider getCredentialsProvider(Map<String, String> keysAndValues) {
        if (keysAndValues != null) {
            String user = keysAndValues.get(Constants.PROXY_AUTH_USER);
            String password = keysAndValues.get(Constants.PROXY_AUTH_PASS);
            if (LocalStringsUtils.isNullOrEmpty(user) && LocalStringsUtils.isNullOrEmpty(password)) {
                return null;
            }

            AuthScope authScope = AuthScope.ANY;

            String host = keysAndValues.get(Constants.PROXY_AUTH_HOST);
            String port = keysAndValues.get(Constants.PROXY_AUTH_PORT);
            String realm = keysAndValues.get(Constants.PROXY_AUTH_REALM);
            String scheme = keysAndValues.get(Constants.PROXT_AUTH_SCHMA);
            if (!LocalStringsUtils.isNullOrEmpty(host)
                    || !LocalStringsUtils.isNullOrEmpty(port)
                    || !LocalStringsUtils.isNullOrEmpty(realm)
                    || !LocalStringsUtils.isNullOrEmpty(scheme)) {

                int portAsInt = AuthScope.ANY_PORT;
                if (!LocalStringsUtils.isNullOrEmpty(port)) {
                    try {
                        portAsInt = Integer.valueOf(port);
                    } catch (NumberFormatException e) {
                        InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Failed to parse proxy port: %s", port);
                        throw e;
                    }
                }
                authScope = new AuthScope(host, portAsInt, realm, scheme);
            }

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(authScope, new UsernamePasswordCredentials(user, password));
            return credentialsProvider;
        }
        return null;
    }
}
