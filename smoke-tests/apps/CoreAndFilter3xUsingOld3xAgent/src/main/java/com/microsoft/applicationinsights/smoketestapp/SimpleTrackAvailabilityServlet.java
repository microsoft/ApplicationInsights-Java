// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.AvailabilityTelemetry;
import com.microsoft.applicationinsights.telemetry.Duration;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/trackAvailability")
public class SimpleTrackAvailabilityServlet extends HttpServlet {

  private final TelemetryClient client = new TelemetryClient();

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    AvailabilityTelemetry telemetry = new AvailabilityTelemetry();
    telemetry.setId("an-id");
    telemetry.setName("a-name");
    telemetry.setDuration(new Duration(1234));
    telemetry.setSuccess(true);
    telemetry.setRunLocation("a-run-location");
    telemetry.setMessage("a-message");

    client.trackAvailability(telemetry);
  }
}
