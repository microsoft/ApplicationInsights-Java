// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.TelemetryClient;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/trackEvent")
public class SimpleTrackEventServlet extends HttpServlet {

  private final TelemetryClient client = new TelemetryClient();

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

    Map<String, String> properties =
        new HashMap<String, String>() {
          {
            put("key", "value");
          }
        };
    Map<String, Double> metrics =
        new HashMap<String, Double>() {
          {
            put("key", 1d);
          }
        };

    // Event
    client.trackEvent("EventDataTest");
    client.trackEvent("EventDataPropertyTest", properties, metrics);
  }
}
