package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

class TargetObject {

  String one() {
    return "One";
  }

  String two() {
    return "Two";
  }

  String two(String input) {
    return input;
  }

  String three() throws Exception {
    throw new Exception("Three");
  }

  String five() {
    return six("Five");
  }

  String six(String arg) {
    one();
    two("two");
    return arg;
  }

  String seven(String arg) {
    return seven("7", "77");
  }

  String seven(String arg1, String arg2) {
    return "Seven";
  }

  String eight(String arg) {
    return eight("8", "88");
  }

  String eight(String arg1, String arg2) {
    return "Eight";
  }

  String nine() throws IOException {
    String url = "https://www.bing.com";
    HttpGet get = new HttpGet(url);
    try (CloseableHttpClient httpClient =
            HttpClientBuilder.create().disableAutomaticRetries().build();
        CloseableHttpResponse response = httpClient.execute(get)) {

      return response.getStatusLine().getReasonPhrase();
    }
  }

  static class NestedObject {

    void four(boolean x, int[] y, String[][] z) {}
  }
}
