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

package com.microsoft.applicationinsights.agent.internal.logger;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.microsoft.applicationinsights.agent.internal.common.StringUtils;

/**
 * Created by gupele on 6/3/2015.
 */
public enum InternalAgentLogger {
    INSTANCE;

    public enum LoggingLevel {
        ALL(Integer.MIN_VALUE),
        TRACE(10000),
        INFO(20000),
        WARN(30000),
        ERROR(40000),
        OFF(50000);

        private int value;

        LoggingLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private boolean initialized = false;

    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ");
    private LoggingLevel loggingLevel = LoggingLevel.OFF;

    public boolean isTraceEnabled() {
        return loggingLevel.getValue() <= LoggingLevel.TRACE.getValue();
    }

    public boolean isInfoEnabled() {
        return loggingLevel.getValue() <= LoggingLevel.INFO.getValue();
    }

    public boolean isWarnEnabled() {
        return loggingLevel.getValue() <= LoggingLevel.WARN.getValue();
    }

    public boolean isErrorEnabled() {
        return loggingLevel.getValue() <= LoggingLevel.ERROR.getValue();
    }

    public synchronized void initialize(String loggerLevel) {
        if (initialized) {
            return;
        }

        initialized = true;

        try {
            if (StringUtils.isNullOrEmpty(loggerLevel)) {
                loggingLevel = LoggingLevel.TRACE;
            } else {
                loggingLevel = LoggingLevel.valueOf(loggerLevel.toUpperCase());
            }
        } catch (Exception e) {
            logAlways(LoggingLevel.ERROR, "Failed to parse logging level, using OFF");
            loggingLevel = LoggingLevel.OFF;
        }
    }

    /**
     * The main method, will delegate the call to the output
     * only if the logger is enabled for errors, will not allow any exception thrown
     * @param message The message to log with possible placeholders.
     * @param args The arguments that should be formatted into the placeholders.
     */
    public void error(String message, Object... args) {
        try {
            log(LoggingLevel.ERROR, message, args);
        } catch (Exception e) {
        }
    }

    /**
     * The main method, will delegate the call to the output
     * only if the logger is enabled for warnings, will not allow any exception thrown
     * @param message The message to log with possible placeholders.
     * @param args The arguments that should be formatted into the placeholders.
     */
    public void warn(String message, Object... args) {
        try {
            log(LoggingLevel.WARN, message, args);
        } catch (Exception e) {
        }
    }

    /**
     * The main method, will delegate the call to the output
     * only if the logger is enabled for info messages, will not allow any exception thrown
     * @param message The message to log with possible placeholders.
     * @param args The arguments that should be formatted into the placeholders.
     */
    public void info(String message, Object... args) {
        try {
            log(LoggingLevel.INFO, message, args);
        } catch (Exception e) {
        }
    }

    /**
     * The main method, will delegate the call to the output
     * only if the logger is enabled for at least trace level, will not allow any exception thrown
     * @param message The message to log with possible placeholders.
     * @param args The arguments that should be formatted into the placeholders.
     */
    public void trace(String message, Object... args) {
        try {
            log(LoggingLevel.TRACE, message, args);
        } catch (Exception e) {
        }
    }

    public void logAlways(LoggingLevel requestLevel, String message, Object... args) {
        String logMessage = createMessage(requestLevel.toString(), message, args);
        System.out.println("AI-Agent: " + logMessage);
    }

    /**
     * Creates the message that contains the prefix, thread id and the message.
     * @param prefix The prefix to attach to the message.
     * @param message The message to write with possible place holders.
     * @param args T The args that are part of the message.
     * @return The formatted message with all the needed data.
     */
    private static String createMessage(String prefix, String message, Object... args) {
        String currentDateAsString;
        synchronized (INSTANCE) {
            currentDateAsString = dateFormatter.format(new Date());
        }
        String formattedMessage = String.format(message, args);
        final Thread thisThread = Thread.currentThread();
        String theMessage = String.format("%s %s, %d(%s): %s", prefix, currentDateAsString, thisThread.getId(), thisThread.getName(), formattedMessage);
        return theMessage;
    }

    private void log(LoggingLevel requestLevel, String message, Object... args) {
        if (requestLevel.getValue() >= loggingLevel.getValue()) {
            System.out.println("AI-Agent: " + createMessage(requestLevel.toString(), message, args));
        }
    }
}
