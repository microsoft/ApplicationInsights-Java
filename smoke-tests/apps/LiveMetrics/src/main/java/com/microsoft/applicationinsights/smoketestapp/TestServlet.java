// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

@WebServlet("/*")
public class TestServlet extends HttpServlet {

  private static final Logger logger = LogManager.getLogger("smoketestapp-livemetrics");

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doWork();
    resp.getWriter().println("ok");
  }

  @WithSpan
  private void doWork() {
    System.out.println("Doing work to generate a dependency call, exception, and trace.");
    logger.error("This message should generate an exception!", new Exception("Fake Exception"));
    logger.info("This message should generate a trace");
  }
}
