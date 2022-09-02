// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/sampling")
public class SimpleSamplingServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(SimpleSamplingServlet.class.getName());

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

    logger.log(Level.WARNING, "test");

    try {
      Thread.sleep(ThreadLocalRandom.current().nextInt(20));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
