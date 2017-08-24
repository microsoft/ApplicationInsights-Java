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
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.entity.ByteArrayEntity;

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
    private final static String RESPONSE_THROTTLING_HEADER = "Retry-After";
    private final static String RESPONSE_RETRY_AFTER_DATE_FORMAT = "E, dd MMM yyyy HH:mm:ss";

    private final static String DEFAULT_SERVER_URI = "https://dc.services.visualstudio.com/v2/track";
    private final static int DEFAULT_BACKOFF_TIME_SECONDS = 300;
    private final static long SHUTDOWN_TIME = 1L;

    // For future use: re-send a failed transmission back to the dispatcher
    private TransmissionDispatcher transmissionDispatcher;

    private final String serverUri;

    private volatile boolean stopped;

    // Use one instance for optimization
    private final ApacheSender httpClient;

    private TransmissionPolicyManager transmissionPolicyManager;

    public static TransmissionNetworkOutput create(TransmissionPolicyManager transmissionPolicyManager) {
        return create(DEFAULT_SERVER_URI, transmissionPolicyManager);
    }

    public static TransmissionNetworkOutput create(String endpoint, TransmissionPolicyManager transmissionPolicyManager) {
        String realEndpoint = Strings.isNullOrEmpty(endpoint) ? DEFAULT_SERVER_URI : endpoint;
        return new TransmissionNetworkOutput(realEndpoint, transmissionPolicyManager);
    }

    private TransmissionNetworkOutput(String serverUri, TransmissionPolicyManager transmissionPolicyManager) {
        Preconditions.checkNotNull(serverUri, "serverUri should be a valid non-null value");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(serverUri), "serverUri should be a valid non-null value");
        Preconditions.checkNotNull(transmissionPolicyManager, "transmissionPolicyManager should be a valid non-null value");

        this.serverUri = serverUri;

        httpClient = ApacheSenderFactory.INSTANCE.create();
        this.transmissionPolicyManager = transmissionPolicyManager;
        stopped = false;
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
            if (transmissionPolicyManager.getTransmissionPolicyState().getCurrentState() != TransmissionPolicy.UNBLOCKED) {
                return false;
            }

            HttpResponse response = null;
            HttpPost request = null;
            boolean shouldBackoff = false;
            try {
                request = createTransmissionPostRequest(transmission);
                httpClient.enhanceRequest(request);

                response = httpClient.sendPostRequest(request);

                HttpEntity respEntity = response.getEntity();
                int code = response.getStatusLine().getStatusCode();

                TransmissionSendResult sendResult = translateResponse(code, respEntity);
                switch (sendResult) {
                    case PAYMENT_REQUIRED:
                    case THROTTLED:
                        suspendTransmissions(TransmissionPolicy.BLOCKED_BUT_CAN_BE_PERSISTED, response);
                        break;

                    case THROTTLED_OVER_EXTENDED_TIME:
                        suspendTransmissions(TransmissionPolicy.BLOCKED_AND_CANNOT_BE_PERSISTED, response);
                        break;

                    default:
                        return true;
                }
            } catch (ConnectionPoolTimeoutException e) {
                InternalLogger.INSTANCE.error("Failed to send, connection pool timeout exception");
                shouldBackoff = true;
            } catch (SocketException e) {
                InternalLogger.INSTANCE.error("Failed to send, socket timeout exception");
                shouldBackoff = true;
            } catch (UnknownHostException e) {
                InternalLogger.INSTANCE.error("Failed to send, wrong host address or cannot reach address due to network issues, exception: %s", e.getMessage());
                shouldBackoff = true;
            } catch (IOException ioe) {
                InternalLogger.INSTANCE.error("Failed to send, exception: %s", ioe.getMessage());
                shouldBackoff = true;
            } catch (IllegalStateException e) {
                InternalLogger.INSTANCE.error("Failed to send, illegal state exception: %s", e.getMessage());
                shouldBackoff = true;
            } catch (Exception e) {
                InternalLogger.INSTANCE.error("Failed to send, unexpected exception: %s", e.getMessage());
                shouldBackoff = true;
            } catch (Throwable t) {
                InternalLogger.INSTANCE.error("Failed to send, unexpected error: %s", t.getMessage());
                shouldBackoff = true;
            }
            finally {
                if (request != null) {
                    request.releaseConnection();
                }
                httpClient.dispose(response);
                // backoff before trying again
                if (shouldBackoff) {
                    transmissionPolicyManager.suspendInSeconds(TransmissionPolicy.BLOCKED_BUT_CAN_BE_PERSISTED, DEFAULT_BACKOFF_TIME_SECONDS);
                }
            }
        }

        return true;
    }

    private void suspendTransmissions(TransmissionPolicy suspensionPolicy, HttpResponse response) {
        Header retryAfterHeader = response.getFirstHeader(RESPONSE_THROTTLING_HEADER);
        if (retryAfterHeader == null) {
            return;
        }

        String retryAfterAsString = retryAfterHeader.getValue();
        if (Strings.isNullOrEmpty(retryAfterAsString)) {
            return;
        }

        try {
            DateFormat formatter = new SimpleDateFormat(RESPONSE_RETRY_AFTER_DATE_FORMAT);
            Date date = formatter.parse(retryAfterAsString);

            Date now = Calendar.getInstance().getTime();
            long retryAfterAsSeconds = (date.getTime() - convertToDateToGmt(now).getTime())/1000;
            transmissionPolicyManager.suspendInSeconds(suspensionPolicy, retryAfterAsSeconds);
        } catch (Throwable e) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Throttled but failed to block transmission, exception: %s", e.getMessage());
        }
    }

    private  static Date convertToDateToGmt(Date date){
        TimeZone tz = TimeZone.getDefault();
        Date ret = new Date(date.getTime() - tz.getRawOffset());

        // If we are now in DST, back off by the delta.  Note that we are checking the GMT date, this is the KEY.
        if (tz.inDaylightTime(ret)) {
            Date dstDate = new Date(ret.getTime() - tz.getDSTSavings());

            // Check to make sure we have not crossed back into standard time
            if (tz.inDaylightTime(dstDate)) {
                ret = dstDate;
            }
        }
        return ret;
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

                case 439:
                    result = TransmissionSendResult.THROTTLED_OVER_EXTENDED_TIME;
                    errorMessage = "Throttling extended";
                    break;

                case 402:
                    result = TransmissionSendResult.PAYMENT_REQUIRED;
                    errorMessage = "Throttling: payment required";
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

        return request;
    }
}
