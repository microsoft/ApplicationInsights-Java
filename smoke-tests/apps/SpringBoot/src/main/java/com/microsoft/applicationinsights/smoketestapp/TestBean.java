// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

@Service
public class TestBean {

  private final CloseableHttpClient httpClient =
      HttpClientBuilder.create().disableAutomaticRetries().build();

  @Async
  public void asyncDependencyCall(DeferredResult<Integer> deferredResult) throws IOException {
    String url = "https://www.bing.com";
    HttpGet get = new HttpGet(url);
    try (CloseableHttpResponse response = httpClient.execute(get)) {
      deferredResult.setResult(response.getStatusLine().getStatusCode());
    }
  }
}
