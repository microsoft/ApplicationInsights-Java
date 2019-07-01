package com.springbootstartertest.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.common.io.ByteStreams;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpecBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

@Service
public class TestBean {

    private CloseableHttpClient httpClient = HttpClientBuilder.create().disableAutomaticRetries().build();

    @Async
    public void asyncDependencyCallWithApacheHttpClient4(DeferredResult<Integer> deferredResult) throws IOException {
        String url = "https://www.bing.com";
        HttpGet get = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            deferredResult.setResult(response.getStatusLine().getStatusCode());
        }
    }

    @Async
    public void asyncDependencyCallWithApacheHttpClient3(DeferredResult<Integer> deferredResult) throws IOException {
        HttpClient httpClient3 = new org.apache.commons.httpclient.HttpClient();
        CookiePolicy.registerCookieSpec("PermitAllCookiesSpec", PermitAllCookiesSpec.class);
        httpClient3.getParams().setCookiePolicy("PermitAllCookiesSpec");
        String url = "https://www.bing.com";
        GetMethod httpGet = new GetMethod(url);
        httpClient3.executeMethod(httpGet);
        httpGet.releaseConnection();
        deferredResult.setResult(httpGet.getStatusCode());
    }

    @Async
    public void asyncDependencyCallWithOkHttp3(DeferredResult<Integer> deferredResult) throws IOException {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://www.bing.com")
                .build();
        okhttp3.Response response = client.newCall(request).execute();
        response.body().close();
        deferredResult.setResult(response.code());
    }

    @Async
    public void asyncDependencyCallWithOkHttp2(DeferredResult<Integer> deferredResult) throws IOException {
        com.squareup.okhttp.OkHttpClient client = new com.squareup.okhttp.OkHttpClient();
        com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                .url("https://www.bing.com")
                .build();
        com.squareup.okhttp.Response response = client.newCall(request).execute();
        response.body().close();
        deferredResult.setResult(response.code());
    }

    @Async
    public void asyncDependencyCallWithHttpURLConnection(DeferredResult<Integer> deferredResult) throws IOException {
        URL obj = new URL("https://www.bing.com");
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        InputStream content = connection.getInputStream();
        ByteStreams.exhaust(content);
        content.close();
        deferredResult.setResult(connection.getResponseCode());
    }

    public static class PermitAllCookiesSpec extends CookieSpecBase {
        public void validate(String host, int port, String path, boolean secure, final Cookie cookie) {
        }
    }
}
