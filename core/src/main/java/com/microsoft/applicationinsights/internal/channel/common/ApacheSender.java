package com.microsoft.applicationinsights.internal.channel.common;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;

/**
 * Created by gupele on 6/4/2015.
 */
interface ApacheSender {
    HttpResponse sendPostRequest(HttpPost post) throws IOException;

    void dispose(HttpResponse response);

    void close();

    HttpClient getHttpClient();
}
