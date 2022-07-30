/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
