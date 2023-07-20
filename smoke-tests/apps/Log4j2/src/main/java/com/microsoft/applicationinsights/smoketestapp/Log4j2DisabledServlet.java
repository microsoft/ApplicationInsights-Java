// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@WebServlet("/testDisabled")
public class Log4j2DisabledServlet extends HttpServlet {

  private static final Logger logger = LogManager.getLogger("smoketestapp");

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    logger.warn("This message will get suppressed");
  }
}
