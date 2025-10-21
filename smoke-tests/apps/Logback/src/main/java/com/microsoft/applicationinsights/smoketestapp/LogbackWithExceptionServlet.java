// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@WebServlet("/testWithException")
public class LogbackWithExceptionServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger("smoketestapp");

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    MDC.put("MDC key", "MDC value");
    Exception e = testNullMessage(request) ? new Exception() : new Exception("Fake Exception");
    logger.error("This is an exception!", e);
    MDC.remove("MDC key");
  }

  private static boolean testNullMessage(HttpServletRequest request) {
    String testNullMessage = request.getParameter("test-null-message");
    return "true".equalsIgnoreCase(testNullMessage);
  }
}
