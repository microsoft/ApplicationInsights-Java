// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

@WebServlet("/traceLog4j1_2WithException")
public class SimpleTestTraceLog4j12WithExceptionServlet extends HttpServlet {

  private static final Logger logger = LogManager.getLogger("smoketestapp");

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    MDC.put("MDC key", "MDC value");
    logger.error("This is an exception!", new Exception("Fake Exception"));
    MDC.remove("MDC key");
  }
}
