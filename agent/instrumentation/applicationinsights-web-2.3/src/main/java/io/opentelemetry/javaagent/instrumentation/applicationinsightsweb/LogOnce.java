// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class LogOnce {

  private static final Logger logger =
      Logger.getLogger("com.microsoft.applicationinsights.interop.web");

  private static final Set<String> loggedMessages =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  public static void logOnce(String message) {
    if (loggedMessages.add(message)) {
      logger.log(WARNING, "{0} (this message will only be logged once)", message);
      if (logger.isLoggable(FINE)) {
        logger.log(
            FINE,
            "location stack trace for the warning above",
            new Exception("location stack trace"));
      }
    }
  }

  private LogOnce() {}
}
