// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/sampling")
public class SimpleSamplingServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(SimpleSamplingServlet.class.getName());

  private final TelemetryClient client = new TelemetryClient();

  private final AtomicInteger count = new AtomicInteger();

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    client.trackEvent(new EventTelemetry("Event Test " + count.getAndIncrement()));
    logger.log(Level.WARNING, "test");
  }
}
