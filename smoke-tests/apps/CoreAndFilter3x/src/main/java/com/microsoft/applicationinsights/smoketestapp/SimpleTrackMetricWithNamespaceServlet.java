// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/trackMetricWithNamespace")
public class SimpleTrackMetricWithNamespaceServlet extends HttpServlet {

  private final TelemetryClient client = new TelemetryClient();

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

    MetricTelemetry metricTelemetry = new MetricTelemetry();
    metricTelemetry.setName("TimeToRespond");
    metricTelemetry.setMetricNamespace("test");
    metricTelemetry.setValue(111222333);

    client.trackMetric(metricTelemetry);
  }
}
