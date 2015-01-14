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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
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
            InternalLogger.INSTANCE.log("Failed to close client, exception: {0}", e.getMessage());
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
            if (code != 200) {
                if (respEntity != null) {
                    logError(respEntity);
                }
            }
        } catch (org.apache.http.conn.ConnectionPoolTimeoutException e) {
            // We let the Dispatcher decide
            transmissionDispatcher.dispatch(transmission);
            InternalLogger.INSTANCE.log("Failed to send, timeout exception");
        } catch (IOException ioe) {
            InternalLogger.INSTANCE.log("Failed to send, exception: {0}", ioe.getMessage());
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException ioeIn) {
                // Returns transmission to the dispatcher
                InternalLogger.INSTANCE.log("Failed to send and failed to close response, exception: {0}", ioeIn.getMessage());
            }
        } catch (Exception e) {
            InternalLogger.INSTANCE.log("Failed to load transmission, file not found, exception: {0}", e.getMessage());
        }
        finally {
            if (request != null) {
                request.releaseConnection();
            }
        }

        return true;
    }

    private void logError(HttpEntity respEntity) {
        InputStream inputStream = null;
        try {
            inputStream = respEntity.getContent();
            InputStreamReader streamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader reader = new BufferedReader(streamReader);
            String responseLine = reader.readLine();
            respEntity.getContent().close();

            InternalLogger.INSTANCE.log("Failed to send: {0}", responseLine);
        } catch (IOException e) {
            InternalLogger.INSTANCE.log("Failed to send and failed to log the error");
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
