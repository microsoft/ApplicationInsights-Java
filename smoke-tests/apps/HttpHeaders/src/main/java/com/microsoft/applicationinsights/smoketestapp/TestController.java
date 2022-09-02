// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/serverHeaders")
  public String serverHeaders(HttpServletResponse response) {
    response.setHeader("abc", "testing123");
    return "OK!";
  }

  @GetMapping("/clientHeaders")
  public String clientHeaders() throws IOException {
    callMockCodes200(false);
    callMockCodes200(true);
    return "OK!";
  }

  private void callMockCodes200(boolean nope) throws IOException {
    URL obj = new URL("https://mock.codes/200");

    HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
    connection.setRequestProperty("abc", nope ? "nope" : "testing123");
    // calling getContentType() first, since this triggered a bug previously in the instrumentation
    // previously
    connection.getContentType();
    InputStream content = connection.getInputStream();
    // drain the content
    byte[] buffer = new byte[1024];
    while (content.read(buffer) != -1) {}
    content.close();
  }
}
