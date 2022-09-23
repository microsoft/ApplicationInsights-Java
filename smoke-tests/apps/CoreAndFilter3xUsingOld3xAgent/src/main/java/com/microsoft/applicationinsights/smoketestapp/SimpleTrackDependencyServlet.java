// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/trackDependency")
public class SimpleTrackDependencyServlet extends HttpServlet {

  private final TelemetryClient client = new TelemetryClient();

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    client.trackDependency("DependencyTest", "commandName", new Duration(0, 0, 1, 1, 1), true);
  }
}
