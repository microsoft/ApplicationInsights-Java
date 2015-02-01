/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.internal.logger;

import com.google.common.base.Strings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * A first, very simple version of an internal logger
 *
 * Note: this class is for the SDK internal use only.
 *
 * By default the logger will not log messages since it will
 * use the 'NullLoggerOutput' and LoggerLevel of 'OFF' which both deny the output
 *
 * TODO: Add logger output implementation, factory, logging Levels? etc.
 *
 * Created by gupele on 1/13/2015.
 */
public enum InternalLogger {
    INSTANCE;

    private final static String LOGGER_LEVEL = "Level";

    public enum LoggingLevel {
        TRACE(0),
        ERROR(1),
        OFF (2);

        private int value;

        LoggingLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum LoggerOutputType {
        CONSOLE,
        FILE
    }

    private boolean initialized = false;

    private LoggingLevel loggingLevel = LoggingLevel.OFF;

    private LoggerOutput loggerOutput = new ConsoleLoggerOutput();

    private InternalLogger() {
    }

    public synchronized void initialize(String loggerOutput, Map<String, String> loggerData) {
        if (!initialized) {
            String loggerLevel = loggerData.remove(LOGGER_LEVEL);
            if (Strings.isNullOrEmpty(loggerLevel)) {
                loggingLevel = LoggingLevel.OFF;
            } else {
                try {
                    loggingLevel = LoggingLevel.valueOf(loggerLevel.toUpperCase());
                } catch (Exception e) {
                    loggingLevel = LoggingLevel.OFF;
                }
            }

            setLoggerOutput(loggerOutput, loggerData);
            initialized = true;
        }
    }

    public boolean isTraceEnabled() {
        return loggingLevel.getValue() <= LoggingLevel.TRACE.getValue();
    }

    public boolean isErrorEnabled() {
        return loggingLevel.getValue() <= LoggingLevel.ERROR.getValue();
    }

    /**
     * The main method, will delegate the call to the output
     * only if the logger is enabled for errors, will not allow any exception thrown
     * @param message The message to log with possible placeholders.
     * @param args The arguments that should be formatted into the placeholders.
     */
    public void error(String message, Object... args) {
        try {
            if (isErrorEnabled()) {
                loggerOutput.log(createMessage("ERROR:", message, args));
            }
        } catch (Throwable t) {
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
            if (isTraceEnabled()) {
                loggerOutput.log(createMessage("TRACE:", message, args));
            }
        } catch (Throwable t) {
        }
    }

    private static String createMessage(String prefix, String message, Object... args) {
        String currentDateAsString = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        String formattedMessage = String.format(message, args);
        String theMessage = String.format("%s %s, %d: %s", prefix, currentDateAsString, Thread.currentThread().getId(), formattedMessage);
        return theMessage;
    }

    private void setLoggerOutput(String loggerOutputType, Map<String, String> loggerData) {
        try {
            LoggerOutputType type = Strings.isNullOrEmpty(loggerOutputType) ? LoggerOutputType.CONSOLE : LoggerOutputType.valueOf(loggerOutputType.toUpperCase());
            switch (type) {
                case CONSOLE:
                    loggerOutput = new ConsoleLoggerOutput();
                    return;

                case FILE:
                    loggerOutput = new FileLoggerOutput(loggerData);
                    return;

                default:
                    return;
            }
        } catch (Exception e) {
        }
    }
}
