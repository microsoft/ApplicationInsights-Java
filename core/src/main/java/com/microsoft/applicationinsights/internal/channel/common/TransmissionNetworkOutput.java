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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.reflect.ClassDataUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

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

    private static SenderThreadsBackOffManager s_senderThreadsManager;

    // For future use: re-send a failed transmission back to the dispatcher
    private TransmissionDispatcher transmissionDispatcher;

    private final String serverUri;

    private volatile boolean stopped;

    // Use one instance for optimization
    private final ApacheSender httpClient;
//    private final CloseableHttpClient httpClient;

    public static TransmissionNetworkOutput create() {
        return create(DEFAULT_SERVER_URI);
    }

    public static TransmissionNetworkOutput create(String endpoint) {
        String realEndpoint = Strings.isNullOrEmpty(endpoint) ? DEFAULT_SERVER_URI : endpoint;
        return new TransmissionNetworkOutput(realEndpoint, null);
    }

    private TransmissionNetworkOutput(String serverUri, String backOffContainerName) {
        Preconditions.checkNotNull(serverUri, "serverUri should be a valid non-null value");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(serverUri), "serverUri should be a valid non-null value");

        this.serverUri = serverUri;

        httpClient = new ApacheSenderFactory().create();
        stopped = false;
        initializeSenderThreadsManager(backOffContainerName);
    }

    public void setTransmissionDispatcher(TransmissionDispatcher transmissionDispatcher) {
        this.transmissionDispatcher = transmissionDispatcher;
    }

    /**
     * Stops all threads from sending data.
     * @param timeout The timeout to wait, which is not relevant here.
     * @param timeUnit The time unit, which is not relevant in this method.
     */
    @Override
    public synchronized void stop(long timeout, TimeUnit timeUnit) {
        if (stopped) {
            return;
        }

        s_senderThreadsManager.stopAllSendersBackOffActivities();
        httpClient.close();
        stopped = true;
    }

    /**
     * Tries to send a {@link com.microsoft.applicationinsights.internal.channel.common.Transmission}
     * The thread that calls that method might be suspended if there is a throttling issues, in any case
     * the thread that enters this method is responsive for 'stop' request that might be issued by the application.
     * @param transmission The data to send
     * @return True when done.
     */
    @Override
    public boolean send(Transmission transmission) {
        while (!stopped) {
            TransmissionSendResult result = doSend(transmission);
            switch (result) {
                case THROTTLED:
                    boolean backOffDone = s_senderThreadsManager.backOffCurrentSenderThread();
                    if (!backOffDone) {
                        return true;
                    }
                    break;

                default:
                   s_senderThreadsManager.onDoneSending();
                   return true;
            }
        }

        return true;
    }

    private TransmissionSendResult doSend(Transmission transmission) {
        HttpResponse response = null;
        HttpPost request = null;
        try {
            request = createTransmissionPostRequest(transmission);

            response = httpClient.sendPostRequest(request);

            HttpEntity respEntity = response.getEntity();
            int code = response.getStatusLine().getStatusCode();

            return translateResponse(code, respEntity);
        } catch (ConnectionPoolTimeoutException e) {
            InternalLogger.INSTANCE.error("Failed to send, connection pool timeout exception");
            return TransmissionSendResult.FAILED_TO_SEND_DUE_TO_CONNECTION_POOL;
        } catch (SocketException e) {
            InternalLogger.INSTANCE.error("Failed to send, socket timeout exception");
            return TransmissionSendResult.FAILED_TO_RECEIVE_DUE_TO_TIMEOUT;
        } catch (UnknownHostException e) {
            InternalLogger.INSTANCE.error("Failed to send, wrong host address or cannot reach address due to network issues, exception: %s", e.getMessage());
            return TransmissionSendResult.FAILED_TO_SEND_DUE_TO_NETWORK_ISSUES;
        } catch (IOException ioe) {
            InternalLogger.INSTANCE.error("Failed to send, exception: %s", ioe.getMessage());
            return TransmissionSendResult.FAILED_TO_READ_RESPONSE;
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to send, unexpected exception: %s", e.getMessage());
            return TransmissionSendResult.UNKNOWN_ERROR;
        } catch (Throwable t) {
            InternalLogger.INSTANCE.error("Failed to send, unexpected error: %s", t.getMessage());
            return TransmissionSendResult.UNKNOWN_ERROR;
        }
        finally {
            if (request != null) {
                request.releaseConnection();
            }
            httpClient.dispose(response);
        }
    }

    private TransmissionSendResult translateResponse(int code, HttpEntity respEntity) {
        if (code == HttpStatus.SC_OK) {
            return TransmissionSendResult.SENT_SUCCESSFULLY;
        }

        TransmissionSendResult result;

        String errorMessage;
        if (code < HttpStatus.SC_OK ||
            (code >= HttpStatus.SC_MULTIPLE_CHOICES && code < HttpStatus.SC_BAD_REQUEST) ||
                code > HttpStatus.SC_INTERNAL_SERVER_ERROR) {

            errorMessage = String.format("Unexpected response code: %d", code);
            result = TransmissionSendResult.REJECTED_BY_SERVER;
        } else {
            switch (code) {
                case HttpStatus.SC_BAD_REQUEST:
                    errorMessage = "Bad request ";
                    result = TransmissionSendResult.BAD_REQUEST;
                    break;

                case 429:
                    result = TransmissionSendResult.THROTTLED;
                    errorMessage = "Throttling (All messages of the transmission were rejected) ";
                    break;

                case HttpStatus.SC_PARTIAL_CONTENT:
                    result = TransmissionSendResult.PARTIALLY_THROTTLED;
                    errorMessage = "Throttling (Partial messages of the transmission were rejected) ";
                    break;

                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    errorMessage = "Internal server error ";
                    result = TransmissionSendResult.INTERNAL_SERVER_ERROR;
                    break;

                default:
                    result = TransmissionSendResult.REJECTED_BY_SERVER;
                    errorMessage = String.format("Error, response code: %d", code);
                    break;
            }
        }

        logError(errorMessage, respEntity);
        return result;
    }

    private void logError(String baseErrorMessage, HttpEntity respEntity) {
        if (respEntity == null || !InternalLogger.INSTANCE.isErrorEnabled()) {
            InternalLogger.INSTANCE.error(baseErrorMessage);
            return;
        }

        InputStream inputStream = null;
        try {
            inputStream = respEntity.getContent();
            InputStreamReader streamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader reader = new BufferedReader(streamReader);
            String responseLine = reader.readLine();
            respEntity.getContent().close();

            InternalLogger.INSTANCE.error("Failed to send, %s : %s", baseErrorMessage, responseLine);
        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to send, %s, failed to log the error", baseErrorMessage);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
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
                .setSocketTimeout(DEFAULT_REQUEST_TIMEOUT_IN_MILLIS)
                .setConnectTimeout(DEFAULT_REQUEST_TIMEOUT_IN_MILLIS)
                .setSocketTimeout(DEFAULT_REQUEST_TIMEOUT_IN_MILLIS).build();

        request.setConfig(requestConfig);

        return request;
    }

    private synchronized void initializeSenderThreadsManager(String backOffContainerName) {
        if (s_senderThreadsManager != null) {
            return;
        }

        s_senderThreadsManager = new SenderThreadsBackOffManager(new BackOffTimesPolicyFactory().create(backOffContainerName));
    }

}
