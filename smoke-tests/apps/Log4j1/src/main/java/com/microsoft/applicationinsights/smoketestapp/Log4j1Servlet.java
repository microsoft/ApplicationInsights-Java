// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.lang.reflect.Field;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.helpers.Loader;

@WebServlet("/test")
public class Log4j1Servlet extends HttpServlet {

  static {
    try {
      // this is needed because log4j1 incorrectly thinks the initial releases of Java 10-19
      // (which have no '.' in their versions since there is no minor version) are Java 1.1,
      // which is before ThreadLocal was introduced and so log4j1 disables MDC functionality
      // (and the MDC tests below fail)
      Field field = Loader.class.getDeclaredField("java1");
      field.setAccessible(true);
      field.set(null, false);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private static final Logger logger = LogManager.getLogger("smoketestapp");

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    logger.trace("This is log4j1.2 trace.");
    logger.debug("This is log4j1.2 debug.");
    logger.info("This is log4j1.2 info.");
    MDC.put("MDC key", "MDC value");
    logger.warn("This is log4j1.2 warn.");
    MDC.remove("MDC key");
    logger.error("This is log4j1.2 error.");
    logger.fatal("This is log4j1.2 fatal.");
  }
}
