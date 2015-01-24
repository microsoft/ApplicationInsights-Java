/*
 * AppInsights-Java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import javax.swing.text.html.HTML;

/**
 * The class is responsible for the actual sending of {@link com.microsoft.applicationinsights.internal.channel.common.Transmission}
 *
 * The class uses Apache's HttpClient framework for that.
 *
 * Created by gupele on 12/18/2014.
 */
public final class TransmissionNetworkOutput implements TransmissionOutput {
    private final static String CONTENT_TYPE_HEADER = "Content-Type";
    private final static String CONTENT_ENCODING_HEADER = "Content-Encoding";
    private final static int DEFAULT_REQUEST_TIMEOUT_IN_MILLIS = 60000;

    private final static String DEFAULT_SERVER_URI = "https://dc.services.visualstudio.com/v2/track";

    private final static int DEFAULT_MAX_TOTAL_CONNECTIONS = 200;
    private final static int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 20;

    // For future use: re-send a failed transmission back to the dispatcher
    private TransmissionDispatcher transmissionDispatcher;

    private final String serverUri;

    // Use one instance for optimization
    private final CloseableHttpClient httpClient;

    public static TransmissionNetworkOutput create() {
        return create(DEFAULT_SERVER_URI);
    }

    public static TransmissionNetworkOutput create(String endpoint) {
        String realEndpoint = Strings.isNullOrEmpty(endpoint) ? DEFAULT_SERVER_URI : endpoint;
        return new TransmissionNetworkOutput(realEndpoint);
    }

    private TransmissionNetworkOutput(String serverUri) {
        Preconditions.checkNotNull(serverUri, "serverUri should be a valid non-null value");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(serverUri), "serverUri should be a valid non-null value");

        this.serverUri = serverUri;

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);

        httpClient = HttpClients.custom().setConnectionManager(cm).build();
    }

    public void setTransmissionDispatcher(TransmissionDispatcher transmissionDispatcher) {
        this.transmissionDispatcher = transmissionDispatcher;
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        try {
            httpClient.close();
        } catch (IOException e) {
            InternalLogger.INSTANCE.log("Failed to close http client, exception: %s", e.getMessage());
        }
    }

    @Override
    public boolean send(Transmission transmission) {
        CloseableHttpResponse response = null;
        HttpPost request = null;
        try {
            request = createTransmissionPostRequest(transmission);

            response = httpClient.execute(request);

            HttpEntity respEntity = response.getEntity();
            int code = response.getStatusLine().getStatusCode();

            if (code != HttpStatus.SC_OK) {
                checkResponse(code, respEntity);
            }
        } catch (org.apache.http.conn.ConnectionPoolTimeoutException e) {
            // We let the Dispatcher decide
            transmissionDispatcher.dispatch(transmission);
            InternalLogger.INSTANCE.log("Failed to send, timeout exception");
        } catch (IOException ioe) {
            InternalLogger.INSTANCE.log("Failed to send, exception: %s", ioe.getMessage());
        } catch (Exception e) {
            InternalLogger.INSTANCE.log("Failed to send, unexpected exception: %s", e.getMessage());
        } catch (Throwable t) {
            InternalLogger.INSTANCE.log("Failed to send, unexpected error: %s", t.getMessage());
        }
        finally {
            if (request != null) {
                request.releaseConnection();
            }
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException ioeIn) {
                InternalLogger.INSTANCE.log("Failed to send or failed to close response, exception: %s", ioeIn.getMessage());
            }
        }

        return true;
    }

    private void checkResponse(int errorCode, HttpEntity respEntity) {
        String errorMessage;
        if (errorCode < HttpStatus.SC_OK ||
            (errorCode >= HttpStatus.SC_MULTIPLE_CHOICES && errorCode < HttpStatus.SC_BAD_REQUEST) ||
                errorCode > HttpStatus.SC_INTERNAL_SERVER_ERROR) {

            errorMessage = String.format("Unexpected response code: %d", errorCode);
        } else {
            switch (errorCode) {
                case 429:
                    errorMessage = "Throttling (All messages of the transmission were rejected) ";
                    break;

                case HttpStatus.SC_PARTIAL_CONTENT:
                    errorMessage = "Throttling (Partial messages of the transmission were rejected) ";
                    break;

                default:
                    errorMessage = String.format("Error, response code: %d", errorCode);
                    break;
            }
        }

        logError(errorMessage, respEntity);
    }

    private void logError(String baseErrorMessage, HttpEntity respEntity) {
        if (respEntity == null) {
            InternalLogger.INSTANCE.log(baseErrorMessage);
            return;
        }

        InputStream inputStream = null;
        try {
            inputStream = respEntity.getContent();
            InputStreamReader streamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader reader = new BufferedReader(streamReader);
            String responseLine = reader.readLine();
            respEntity.getContent().close();

            InternalLogger.INSTANCE.log("Failed to send, %s : %s", baseErrorMessage, responseLine);
        } catch (IOException e) {
            InternalLogger.INSTANCE.log("Failed to send, %s, failed to log the error", baseErrorMessage);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private HttpPost createTransmissionPostRequest(Transmission transmission) {
        HttpPost request = new HttpPost(serverUri);
        request.addHeader(CONTENT_TYPE_HEADER, transmission.getWebContentType());
        request.addHeader(CONTENT_ENCODING_HEADER, transmission.getWebContentEncodingType());

        ByteArrayEntity bae = new ByteArrayEntity(transmission.getContent());
        request.setEntity(bae);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(DEFAULT_REQUEST_TIMEOUT_IN_MILLIS)
                .setConnectTimeout(DEFAULT_REQUEST_TIMEOUT_IN_MILLIS)
                .setSocketTimeout(DEFAULT_REQUEST_TIMEOUT_IN_MILLIS).build();

        request.setConfig(requestConfig);

        return request;
    }
}
