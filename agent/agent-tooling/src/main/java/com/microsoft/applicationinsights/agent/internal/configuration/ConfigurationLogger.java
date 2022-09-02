// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;

class ConfigurationLogger {

  // cannot use logger before loading configuration, so need to store warning messages locally until
  // logger is initialized
  private final List<Message> messages = new CopyOnWriteArrayList<>();

  void warn(String message, Object... args) {
    messages.add(new Message(ConfigurationLogLevel.WARN, message, args));
  }

  void debug(String message, Object... args) {
    messages.add(new Message(ConfigurationLogLevel.DEBUG, message, args));
  }

  void log(Logger logger) {
    for (Message message : messages) {
      message.log(logger);
    }
  }

  private static class Message {
    private final ConfigurationLogLevel level;
    private final String message;
    private final Object[] args;

    private Message(ConfigurationLogLevel level, String message, Object... args) {
      this.level = level;
      this.message = message;
      this.args = args;
    }

    private void log(Logger logger) {
      level.log(logger, message, args);
    }
  }

  private enum ConfigurationLogLevel {
    WARN {
      @Override
      public void log(Logger logger, String message, Object... args) {
        logger.warn(message, args);
      }
    },
    DEBUG {
      @Override
      public void log(Logger logger, String message, Object... args) {
        logger.debug(message, args);
      }
    };

    public abstract void log(Logger logger, String message, Object... args);
  }
}
