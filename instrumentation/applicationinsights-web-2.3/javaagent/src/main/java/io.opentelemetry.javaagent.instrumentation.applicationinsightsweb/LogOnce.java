/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogOnce {

  private static final Logger logger =
      LoggerFactory.getLogger("com.microsoft.applicationinsights.interop.web");

  private static final Set<String> loggedMessages =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  public static void logOnce(String message) {
    if (loggedMessages.add(message)) {
      logger.warn("{} (this message will only be logged once)", message);
      if (logger.isDebugEnabled()) {
        logger.debug(
            "location stack trace for the warning above", new Exception("location stack trace"));
      }
    }
  }
}
