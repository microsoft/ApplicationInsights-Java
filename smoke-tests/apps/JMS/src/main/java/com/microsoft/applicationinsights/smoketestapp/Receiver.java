// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class Receiver {

  @JmsListener(destination = "message", containerFactory = "factory")
  public void message(String message) throws IOException {
    System.out.println("received: " + message);

    CloseableHttpClient httpClient = HttpClientBuilder.create().disableAutomaticRetries().build();
    httpClient.execute(new HttpGet("https://www.bing.com")).close();
  }
}
