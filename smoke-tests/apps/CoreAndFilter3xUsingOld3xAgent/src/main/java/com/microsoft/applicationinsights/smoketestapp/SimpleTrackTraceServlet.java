// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/trackTrace")
public class SimpleTrackTraceServlet extends HttpServlet {

  private final TelemetryClient client = new TelemetryClient();

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    Map<String, String> properties =
        new HashMap<String, String>() {
          {
            put("key", "value");
          }
        };
    // Trace
    client.trackTrace("This is first trace message.");
    client.trackTrace("This is second trace message.", SeverityLevel.Error, null);
    client.trackTrace("This is third trace message.", SeverityLevel.Information, properties);
  }
}
