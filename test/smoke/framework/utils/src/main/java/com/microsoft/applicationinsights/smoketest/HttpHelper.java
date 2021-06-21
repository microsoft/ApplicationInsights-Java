package com.microsoft.applicationinsights.smoketest;

import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class HttpHelper {

    public static int getResponseCodeEnsuringSampled(String url) throws UnsupportedOperationException, IOException {
        HttpGet httpGet = new HttpGet(url);
        // traceId=27272727272727272727272727272727 is known to produce a score of 0.66 (out of 100)
        // so will be sampled as long as samplingPercentage > 1%
        httpGet.setHeader("traceparent", "00-27272727272727272727272727272727-1111111111111111-01");
        return getResponseCode(httpGet);
    }

    public static String get(String url) throws UnsupportedOperationException, IOException {
        return getBody(new HttpGet(url));
    }

    private static String getBody(HttpGet httpGet) throws UnsupportedOperationException, IOException {
        try (CloseableHttpClient client = getHttpClient()) {
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    private static int getResponseCode(HttpGet httpGet) throws UnsupportedOperationException, IOException {
        try (CloseableHttpClient client = getHttpClient()) {
            CloseableHttpResponse resp1 = client.execute(httpGet);
            EntityUtils.consume(resp1.getEntity());
            return resp1.getStatusLine().getStatusCode();
        }
    }

    private static CloseableHttpClient getHttpClient() {
        return HttpClientBuilder.create()
                .disableAutomaticRetries()
                .build();
    }

    public static String post(String url, String body) throws IOException {
        try (CloseableHttpClient client = getHttpClient()) {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(body));
            try (CloseableHttpResponse response = client.execute(post)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }
}