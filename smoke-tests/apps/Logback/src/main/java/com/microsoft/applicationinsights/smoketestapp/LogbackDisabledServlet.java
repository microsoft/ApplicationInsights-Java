// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/testDisabled")
public class LogbackDisabledServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger("smoketestapp");

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    logger.warn("This message will get suppressed");
  }
}
