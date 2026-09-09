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
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@WebServlet("/test")
public class LogbackFluentLoggingServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger("smoketestapp");

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    Marker marker = MarkerFactory.getMarker("aMarker");
    MDC.put("MDC key", "MDC value");
    logger.atTrace().addKeyValue("customKey", "customValue").addMarker(marker).log("This is logback trace.");
    logger.atDebug().addKeyValue("customKey", "customValue").addMarker(marker).log("This is logback debug.");
    logger.atInfo().addKeyValue("customKey", "customValue").addMarker(marker).log("This is logback info.");
    logger.atWarn().addKeyValue("customKey", "customValue").addMarker(marker).log("This is logback warn.");
    logger.atError().addKeyValue("customKey", "customValue").addMarker(marker).log("This is logback error.");
    MDC.remove("MDC key");
  }
}
