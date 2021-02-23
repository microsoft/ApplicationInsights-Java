package com.microsoft.applicationinsights.smoketest;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
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
            CloseableHttpResponse resp1 = client.execute(httpGet);
            return extractResponseBody(resp1);
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

    public static String post(String url, String body) throws ClientProtocolException, IOException {
        CloseableHttpClient client = getHttpClient();
        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity("PING"));
            CloseableHttpResponse resp1 = client.execute(post);
            return extractResponseBody(resp1);
        }
        finally {
            client.close();
        }
    }

    private static String extractResponseBody(CloseableHttpResponse resp) throws IOException {
        try {
            HttpEntity entity = resp.getEntity();
            StringWriter cw = new StringWriter();
            CharStreams.copy(new InputStreamReader(entity.getContent()), cw);
            EntityUtils.consume(entity);
            return cw.toString();
        }
        finally {
            resp.close();
        }
    }
}