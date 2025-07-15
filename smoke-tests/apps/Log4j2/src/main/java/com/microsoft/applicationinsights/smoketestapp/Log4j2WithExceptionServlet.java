// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

@WebServlet("/testWithException")
public class Log4j2WithExceptionServlet extends HttpServlet {

  private static final Logger logger = LogManager.getLogger("smoketestapp");

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    ThreadContext.put("MDC key", "MDC value");
    Exception e = testNullMessage(request) ? new Exception() : new Exception("Fake Exception");
    logger.error("This is an exception!", e);
    ThreadContext.remove("MDC key");
  }

  private static boolean testNullMessage(HttpServletRequest request) {
    String testNullMessage = request.getParameter("test-null-message");
    return "true".equalsIgnoreCase(testNullMessage);
  }
}
