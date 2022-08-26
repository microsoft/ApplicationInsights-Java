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
public class HttpPreaggregatedMetricsTestServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
    try {
      doGetInternal(req);
      resp.getWriter().println("hi!");
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  // "/httpUrlConnection"
  private void doGetInternal(HttpServletRequest req) throws Exception {
    String pathInfo = req.getPathInfo();
    final ExecuteGetUrl executeGetUrl;
    switch (pathInfo) {
      case "/":
        executeGetUrl = null;
        break;
      case "/httpUrlConnection":
        executeGetUrl = this::httpUrlConnection;
        break;
      default:
        throw new ServletException("Unexpected url: " + pathInfo);
    }

    if (executeGetUrl != null) {
      executeGetUrl.execute("https://mock.codes/200?q=spaces%20test");
      try {
        executeGetUrl.execute("https://mock.codes/404");
      } catch (Exception e) {
        // HttpURLConnection throws exception on 404 and 500
      }
      try {
        executeGetUrl.execute("https://mock.codes/500");
      } catch (Exception e) {
        // HttpURLConnection throws exception on 404 and 500
      }
    }
  }

  private void httpUrlConnection(String url) throws IOException {
    URL obj = new URL(url);
    HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
    // calling getContentType() first, since this triggered a bug previously in the instrumentation
    // previously
    connection.getContentType();
    InputStream content = connection.getInputStream();
    // drain the content
    byte[] buffer = new byte[1024];
    while (content.read(buffer) != -1) {}
    content.close();
  }

  @FunctionalInterface
  interface ExecuteGetUrl {
    void execute(String url) throws Exception;
  }
}
