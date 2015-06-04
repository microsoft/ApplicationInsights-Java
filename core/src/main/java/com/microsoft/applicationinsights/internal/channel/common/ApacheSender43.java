package com.microsoft.applicationinsights.internal.channel.common;

import java.io.IOException;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * Created by gupele on 6/4/2015.
 */
final class ApacheSender43 implements ApacheSender {
    private final static int DEFAULT_MAX_TOTAL_CONNECTIONS = 200;
    private final static int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 20;

    private final CloseableHttpClient httpClient;

    public ApacheSender43() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);

        httpClient = HttpClients.custom().setConnectionManager(cm).build();
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
}
