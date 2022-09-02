// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class HttpPreaggregatedMetricsServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String pathInfo = req.getPathInfo();
    if (pathInfo.equals("/")) {
      return;
    }
    if (!pathInfo.equals("/httpUrlConnection")) {
      throw new ServletException("Unexpected url: " + pathInfo);
    }
    httpUrlConnection("https://mock.codes/200?q=spaces%20test");
    try {
      httpUrlConnection("https://mock.codes/404");
    } catch (Exception e) {
      // HttpURLConnection throws exception on 404 and 500
    }
    try {
      httpUrlConnection("https://mock.codes/500");
    } catch (Exception e) {
      // HttpURLConnection throws exception on 404 and 500
    }
  }

  private void httpUrlConnection(String url) throws IOException {
    URL obj = new URL(url);
    HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
    InputStream content = connection.getInputStream();
    // drain the content
    byte[] buffer = new byte[1024];
    while (content.read(buffer) != -1) {}
    content.close();
  }
}
