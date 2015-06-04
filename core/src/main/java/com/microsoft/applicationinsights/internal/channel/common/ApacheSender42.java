package com.microsoft.applicationinsights.internal.channel.common;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

/**
 * Created by gupele on 6/4/2015.
 */
final class ApacheSender42 implements ApacheSender {
    private HttpClient httpClient;

    public ApacheSender42() {
        httpClient = new DefaultHttpClient();
    }

    @Override
    public HttpResponse sendPostRequest(HttpPost post) throws IOException {
        httpClient.execute(post);
        return null;
    }

    @Override
    public void dispose(HttpResponse response) {
    }

    @Override
    public void close() {
    }

    @Override
    public HttpClient getHttpClient() {
        return httpClient;
    }
}
