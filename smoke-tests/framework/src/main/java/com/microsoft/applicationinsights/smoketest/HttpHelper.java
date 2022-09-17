// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class HttpHelper {

  public static int getResponseCodeEnsuringSampled(String url) throws IOException {
    HttpGet httpGet = new HttpGet(url);
    // traceId=27272727272727272727272727272727 is known to produce a score of 0.66 (out of 100)
    // so will be sampled as long as samplingPercentage > 1%
    httpGet.setHeader("traceparent", "00-27272727272727272727272727272727-1111111111111111-01");
    return getResponseCode(httpGet);
  }

  public static String get(String url, String userAgent) throws IOException {
    HttpGet httpGet = new HttpGet(url);
    if (!userAgent.isEmpty()) {
      httpGet.setHeader("User-Agent", userAgent);
    }
    return getBody(httpGet);
  }

  private static String getBody(HttpGet httpGet) throws IOException {
    try (CloseableHttpClient client = getHttpClient()) {
      try (CloseableHttpResponse response = client.execute(httpGet)) {
        return EntityUtils.toString(response.getEntity());
      }
    }
  }

  private static int getResponseCode(HttpGet httpGet) throws IOException {
    try (CloseableHttpClient client = getHttpClient()) {
      CloseableHttpResponse resp1 = client.execute(httpGet);
      EntityUtils.consume(resp1.getEntity());
      return resp1.getStatusLine().getStatusCode();
    }
  }

  private static CloseableHttpClient getHttpClient() {
    return HttpClientBuilder.create().disableAutomaticRetries().build();
  }

  private HttpHelper() {}
}
