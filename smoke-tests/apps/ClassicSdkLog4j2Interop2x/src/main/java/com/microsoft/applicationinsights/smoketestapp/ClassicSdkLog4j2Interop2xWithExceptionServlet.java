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
public class ClassicSdkLog4j2Interop2xWithExceptionServlet extends HttpServlet {

  private static final Logger logger = LogManager.getLogger("smoketestapp");

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    ThreadContext.put("MDC key", "MDC value");
    logger.error("This is an exception!", new Exception("Fake Exception"));
    ThreadContext.remove("MDC key");
  }
}
