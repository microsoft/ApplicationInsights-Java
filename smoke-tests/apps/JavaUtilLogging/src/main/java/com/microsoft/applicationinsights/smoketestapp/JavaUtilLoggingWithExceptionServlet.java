// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/testWithException")
public class JavaUtilLoggingWithExceptionServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger("smoketestapp");

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    Exception e = testNullMessage(request) ? new Exception() : new Exception("Fake Exception");
    logger.log(Level.SEVERE, "This is an exception!", e);
  }

  private static boolean testNullMessage(HttpServletRequest request) {
    String testNullMessage = request.getParameter("test-null-message");
    return "true".equalsIgnoreCase(testNullMessage);
  }
}
