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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.channel.TransmissionOutputSync;
import com.microsoft.applicationinsights.internal.statsbeat.NetworkStatsbeat;
import com.microsoft.applicationinsights.internal.util.ExceptionStats;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class is responsible for the actual sending of
 * {@link com.microsoft.applicationinsights.internal.channel.common.Transmission}
 *
 * The class uses Apache's HttpClient framework for that.
 *
 * Created by gupele on 12/18/2014.
 */
public final class TransmissionNetworkOutput implements TransmissionOutputSync {

    private static final Logger logger = LoggerFactory.getLogger(TransmissionNetworkOutput.class);
    private static final AtomicBoolean friendlyExceptionThrown = new AtomicBoolean();
    private static final ExceptionStats networkExceptionStats = new ExceptionStats(
            TransmissionNetworkOutput.class,
            "Unable to send telemetry to the ingestion service (telemetry will be stored to disk):");

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
    private static final String RESPONSE_THROTTLING_HEADER = "Retry-After";

    public static final String DEFAULT_SERVER_URI = "https://dc.services.visualstudio.com/v2/track";

    // For future use: re-send a failed transmission back to the dispatcher
    private TransmissionDispatcher transmissionDispatcher;

    private final String serverUri;

    private final TelemetryConfiguration configuration;

    // Use one instance for optimization
    private final HttpClient httpClient;

    private final TransmissionPolicyManager transmissionPolicyManager;

    public static TransmissionNetworkOutput create(TelemetryConfiguration configuration, TransmissionPolicyManager transmissionPolicyManager) {
        return new TransmissionNetworkOutput(null, configuration, transmissionPolicyManager);
    }

    private TransmissionNetworkOutput(@Nullable String serverUri, @Nullable TelemetryConfiguration configuration, TransmissionPolicyManager transmissionPolicyManager) {
        Preconditions.checkNotNull(transmissionPolicyManager, "transmissionPolicyManager should be a valid non-null value");
        this.serverUri = serverUri;
        this.configuration = configuration;
        if (StringUtils.isNotEmpty(serverUri)) {
            logger.warn("Setting the endpoint via the <Channel> element is deprecated and will be removed in a future version. Use the top-level element <ConnectionString>.");
        }
        httpClient = LazyHttpClient.getInstance();
        this.transmissionPolicyManager = transmissionPolicyManager;
        if (logger.isTraceEnabled()) {
            logger.trace("{} using endpoint {}", TransmissionNetworkOutput.class.getSimpleName(), getIngestionEndpoint());
        }
    }

    public static TransmissionNetworkOutput create(@Nullable String endpoint, TransmissionPolicyManager transmissionPolicyManager) {
        return new TransmissionNetworkOutput(endpoint, null, transmissionPolicyManager);
    }
    /**
     * Used to inject the dispatcher used for this output so it can be injected to the retry logic.
     *
     * @param transmissionDispatcher The dispatcher to be injected.
     */
    public void setTransmissionDispatcher(TransmissionDispatcher transmissionDispatcher) {
        this.transmissionDispatcher = transmissionDispatcher;
    }

    /**
     * Tries to send a
     * {@link com.microsoft.applicationinsights.internal.channel.common.Transmission}
     * The thread that calls that method might be suspended if there is a throttling
     * issues, in any case the thread that enters this method is responsive for
     * 'stop' request that might be issued by the application.
     *
     * @param transmission
     *            The data to send
     * @return True when done.
     */
    @Override
    public boolean sendSync(Transmission transmission) {
        // If we're not stopped but in a blocked state then fail to second transmission output
        if (transmissionPolicyManager.getTransmissionPolicyState().getCurrentState() != TransmissionPolicy.UNBLOCKED) {
            return false;
        }

        HttpResponse response = null;
        HttpPost request = null;
        int code = 0;
        String reason = null;
        String respString = null;
        Throwable ex = null;
        Header retryAfterHeader = null;
        try {
            // POST the transmission data to the endpoint
            request = createTransmissionPostRequest(transmission);
            response = httpClient.execute(request);
            HttpEntity respEntity = response.getEntity();
            code = response.getStatusLine().getStatusCode();
            reason = response.getStatusLine().getReasonPhrase();
            respString = EntityUtils.toString(respEntity);
            retryAfterHeader = response.getFirstHeader(RESPONSE_THROTTLING_HEADER);

            // After we reach our instant retry limit we should fail to second transmission output
            if (code > HttpStatus.SC_PARTIAL_CONTENT && transmission.getNumberOfSends() > this.transmissionPolicyManager.getMaxInstantRetries()) {
                return false;
            } else if (code == HttpStatus.SC_OK) {
                // If we've completed then clear the back off flags as the channel does not need
                // to be throttled
                transmissionPolicyManager.clearBackoff();
                // Increment Success Counter
                networkExceptionStats.recordSuccess();
                NetworkStatsbeat.incrementRequestSuccessCount();
            }
            return true;
        } catch (ConnectionPoolTimeoutException e) {
            networkExceptionStats.recordFailure("connection pool timeout exception: " + e, e);
            NetworkStatsbeat.incrementRequestFailureCount();
        } catch (SocketException e) {
            networkExceptionStats.recordFailure("socket exception: " + e, e);
            NetworkStatsbeat.incrementRequestFailureCount();
        } catch (SocketTimeoutException e) {
            networkExceptionStats.recordFailure("socket timeout exception: " + e, e);
            NetworkStatsbeat.incrementRequestFailureCount();
        } catch (UnknownHostException e) {
            networkExceptionStats.recordFailure("wrong host address or cannot reach address due to network issues: " + e, e);
            NetworkStatsbeat.incrementRequestFailureCount();
        } catch (IOException e) {
            networkExceptionStats.recordFailure("I/O exception: " + e, e);
            NetworkStatsbeat.incrementRequestFailureCount();
        } catch (FriendlyException e) {
            ex = e;
            // TODO should this be merged into networkExceptionStats?
            if(!friendlyExceptionThrown.getAndSet(true)) {
                logger.error(e.getMessage());
            }
            NetworkStatsbeat.incrementRequestFailureCount();
        } catch (Exception e) {
            networkExceptionStats.recordFailure("unexpected exception: " + e, e);
            NetworkStatsbeat.incrementRequestFailureCount();
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            ex = t;
            try {
                networkExceptionStats.recordFailure("unexpected exception: " + t, t);
                NetworkStatsbeat.incrementRequestFailureCount();
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
            LazyHttpClient.dispose(response);

            if (code == HttpStatus.SC_BAD_REQUEST) {
                networkExceptionStats.recordFailure("ingestion service returned 400 (" + reason + ")");
                NetworkStatsbeat.incrementRequestFailureCount();
            } else if (code != HttpStatus.SC_OK) {
                // Invoke the listeners for handling things like errors
                // The listeners will handle the back off logic as well as the dispatch
                // operation
                TransmissionHandlerArgs args = new TransmissionHandlerArgs();
                args.setTransmission(transmission);
                args.setTransmissionDispatcher(transmissionDispatcher);
                args.setResponseBody(respString);
                args.setResponseCode(code);
                args.setException(ex);
                args.setRetryHeader(retryAfterHeader);
                this.transmissionPolicyManager.onTransmissionSent(args);
            }
        }
        // If we end up here we've hit an error code we do not expect (403, 401, 400,
        // etc.)
        // This also means that unless there is a TransmissionHandler for this code we
        // will not retry.
        return true;
    }

    /**
     * Generates the HTTP POST to send to the endpoint.
     *
     * @param transmission The transmission to send.
     * @return The completed {@link HttpPost} object
     */
    private HttpPost createTransmissionPostRequest(Transmission transmission) {
        HttpPost request = new HttpPost(getIngestionEndpoint());
        request.addHeader(CONTENT_TYPE_HEADER, transmission.getWebContentType());
        request.addHeader(CONTENT_ENCODING_HEADER, transmission.getWebContentEncodingType());

        ByteArrayEntity bae = new ByteArrayEntity(transmission.getContent());
        request.setEntity(bae);

        return request;
    }

    private String getIngestionEndpoint() {
        if (serverUri != null) {
            return serverUri;
        } else if (configuration != null) {
            return configuration.getEndpointProvider().getIngestionEndpointURL().toString();
        } else {
            return DEFAULT_SERVER_URI;
        }
    }
}
