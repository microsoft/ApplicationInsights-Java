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

@WebServlet("/test")
public class ClassicSdkLogbackInterop2xServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger("smoketestapp");

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    logger.trace("This is logback trace.");
    logger.debug("This is logback debug.");
    logger.info("This is logback info.");
    MDC.put("MDC key", "MDC value");
    logger.warn("This is logback warn.");
    MDC.remove("MDC key");
    logger.error("This is logback error.");
  }
}
