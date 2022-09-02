// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.util.concurrent.TimeUnit;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/requestSlow")
public class SimpleTestRequestSlowWithResponseTime extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

    int sleepTime = 25;
    String customSleepTime = request.getParameter("sleeptime");
    if (customSleepTime != null) {
      try {
        sleepTime = Integer.parseInt(customSleepTime);
      } catch (NumberFormatException e) {
        System.err.printf("Invalid value for 'sleeptime': '%s'%n", customSleepTime);
      }
    }
    try {
      System.out.printf("Sleeping for %d seconds.%n", sleepTime);
      TimeUnit.SECONDS.sleep(sleepTime);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
