package com.microsoft.applicationinsights.channel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;


/**
 * The class is responsible for the actual sending of {@link com.microsoft.applicationinsights.channel.Transmission}
 *
 * The class uses Apache's HttpClient framework for that.
 *
 * Created by gupele on 12/18/2014.
 */
public final class TransmissionNetworkOutput implements TransmissionOutput {
    private final static String CONTENT_TYPE_HEADER = "Content-Type:";
    private final static String CONTENT_ENCODING_HEADER = "Content-Encoding:";
    private final static int DEFAULT_REQUEST_TIMEOUT_IN_MILLIS = 60000;

    private final static String DEFAULT_SERVER_URI = "https://dc.services.visualstudio.com/v2/track";

    // For future use: re-send a failed transmission back to the dispatcher
    private TransmissionDispatcher transmissionDispatcher;

    private final String serverUri;

    // Use one instance for optimization
    private final CloseableHttpClient httpClient;

    public TransmissionNetworkOutput() {
        this(DEFAULT_SERVER_URI);
    }

    public TransmissionNetworkOutput(String serverUri) {
        Preconditions.checkNotNull(serverUri, "serverUri should be a valid non-null value");
        Preconditions.checkArgument(!"".equals(serverUri), "serverUri should be a valid non-null value");

        this.serverUri = serverUri;

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(20);

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
            e.printStackTrace();
        }
    }

    @Override
    public boolean send(Transmission transmission) {
        CloseableHttpResponse response = null;
        try {
            HttpPost request = createTransmissionPostRequest(transmission);

            response = httpClient.execute(request);

            HttpEntity respEntity = response.getEntity();
            if (respEntity != null) {
                respEntity.getContent().close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException ioeIn) {
                ioeIn.printStackTrace(System.err);
            }
        }

        return true;
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
