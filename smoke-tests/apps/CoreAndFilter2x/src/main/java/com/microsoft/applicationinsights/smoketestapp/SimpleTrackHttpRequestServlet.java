// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import java.net.MalformedURLException;
import java.util.Date;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/trackHttpRequest")
public class SimpleTrackHttpRequestServlet extends HttpServlet {

  private final TelemetryClient client = new TelemetryClient();

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws MalformedURLException {

    // true
    client.trackHttpRequest("HttpRequestDataTest", new Date(), 4711, "200", true);

    RequestTelemetry rt = new RequestTelemetry("PingTest", new Date(), 1, "200", true);
    rt.setUrl("http://tempuri.org/ping");
    client.trackRequest(rt);

    // false
    client.trackHttpRequest("FailedHttpRequest", new Date(), 6666, "404", false);

    RequestTelemetry rt2 =
        new RequestTelemetry("FailedHttpRequest2", new Date(), 8888, "505", false);
    rt2.setUrl("https://www.bingasdasdasdasda.com/");
    client.trackRequest(rt2);
  }
}
