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

  public static int getResponseCodeEnsuringSampled(String url) throws IOException {
    HttpGet httpGet = new HttpGet(url);
    // traceId=27272727272727272727272727272727 is known to produce a score of 0.66 (out of 100)
    // so will be sampled as long as samplingPercentage > 1%
    httpGet.setHeader("traceparent", "00-27272727272727272727272727272727-1111111111111111-01");
    return getResponseCode(httpGet);
  }

  public static String get(String url) throws IOException {
    return getBody(new HttpGet(url));
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

  public static String post(String url, String body) throws IOException {
    try (CloseableHttpClient client = getHttpClient()) {
      HttpPost post = new HttpPost(url);
      post.setEntity(new StringEntity(body));
      try (CloseableHttpResponse response = client.execute(post)) {
        return EntityUtils.toString(response.getEntity());
      }
    }
  }

  private HttpHelper() {}
}
