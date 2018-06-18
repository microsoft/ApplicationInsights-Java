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

package com.microsoft.applicationinsights.internal.logger;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import com.google.common.base.Strings;

/**
 * A first, very simple version of an internal logger
 *
 * Note: this class is for the SDK internal use only, and therefore should ONLY be used by SDK classes.
 *
 * By default the logger will not log messages since it will
 * use the 'NullLoggerOutput' and LoggerLevel of 'OFF' which both deny the output
 *
 * Created by gupele on 1/13/2015.
 */
public enum InternalLogger {
    INSTANCE;

    private final static String LOGGER_LEVEL = "Level";
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");

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

    public enum LoggerOutputType {
        CONSOLE,
        FILE
    }

    private boolean initialized = false;

    private LoggingLevel loggingLevel = LoggingLevel.OFF;

    private LoggerOutput loggerOutput = null;

    private InternalLogger() {
    }

    /**
     * The method will first try to find the logger level and then the logger type.
     * Note that if there are problems initializing the data the internal logger will
     * move to its default state which is LoggerLevel.OFF and no logger output (loggerOutput = null).
     * @param loggerOutputType The requested logger type
     * @param loggerData The data for the internal logger
     */
    public synchronized void initialize(String loggerOutputType, Map<String, String> loggerData) {
        if (!initialized) {
            try {
                String loggerLevel = loggerData.remove(LOGGER_LEVEL);
                if (Strings.isNullOrEmpty(loggerLevel)) {
                    // The user didn't specify the logging level, therefore by default we set that to 'TRACE'
                    loggingLevel = LoggingLevel.TRACE;
                    setLoggerOutput(loggerOutputType, loggerData);
                } else {
                    try {
                        // Try to match the user request logging level into our enum.
                        loggingLevel = LoggingLevel.valueOf(loggerLevel.toUpperCase());
                        setLoggerOutput(loggerOutputType, loggerData);
                    } catch (Exception e) {
                        // Failed
                        onInitializationError(String.format("Error: Illegal value '%s' for the SDK internal logger. Logging level is therefore set to 'OFF'", loggerLevel));
                    }
                }

                final String utcId = "UTC";
                try {
                    dateFormatter.setTimeZone(TimeZone.getTimeZone(utcId));
                } catch (Exception e) {
                    new ConsoleLoggerOutput().log(String.format("Failed to find timezone with id='%s'. Using default '%s'", utcId, dateFormatter.getTimeZone().getDisplayName()));
                }
            } finally {
                initialized = true;
            }
        }
    }

    /**
     * Closes the Internal Logger for messages.
     * This method should only be called when the internal logger is not needed
     * which is currently prior to the process exits, i.e. prior to shutdown of the process
     */
    public synchronized void stop() {
        if (loggingLevel.equals(LoggingLevel.OFF)) {
            return;
        }

        try {
            if (loggerOutput != null) {
                loggerOutput.close();
            }
        } catch (Exception e) {
        }

        // Prevent further logging of messages
        loggingLevel = LoggingLevel.OFF;
    }

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

    /**
     * The method will log the message in any case. It the logger is not initialized it will print to the console
     * Otherwise the method will use the current logger but will print the message regardless of the logging level.
     * The method is needed for publishing messages when the logger initialization data is unknown
     * Either, before reading the file, or when there was error reading that configuration file, use with care!
     * @param requestLevel - The level of the message
     * @param message - The message to print
     * @param args - The arguments that are part of the message
     */
    public void logAlways(LoggingLevel requestLevel, String message, Object... args) {
        String logMessage = createMessage(requestLevel.toString(), message, args);
        if (!initialized || loggerOutput == null) {
            new ConsoleLoggerOutput().log(logMessage);
        } else {
            loggerOutput.log(logMessage);
        }
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

    /**
     * Setting the output is only relevant if the logging level is different than LoggingLevel.OFF.
     * If the loggerOutputType is empty, then we will use the default logger which is the CONSOLE logger.
     * @param loggerOutputType The requested logger to use, if empty we will use the default, CONSOLE.
     * @param loggerData The data that might be relevant for the logger output
     */
    private void setLoggerOutput(String loggerOutputType, Map<String, String> loggerData) {
        if (loggingLevel.equals(LoggingLevel.OFF)) {
            loggerOutput = null;
            return;
        }

        LoggerOutputType type = LoggerOutputType.CONSOLE;
        if (!Strings.isNullOrEmpty(loggerOutputType)) {
            try {
                // If the user asked for a logger type
                type = LoggerOutputType.valueOf(loggerOutputType.toUpperCase());
            } catch (Exception e) {
                onInitializationError(String.format("Error: Illegal value '%s' for the SDK Internal Logger type.", loggerOutputType));
                return;
            }
        }

        switch (type) {
            case CONSOLE:
                loggerOutput = new ConsoleLoggerOutput();
                return;

            case FILE:
                try {
                    loggerOutput = new FileLoggerOutput(loggerData);
                } catch (Exception e) {
                    onInitializationError(String.format("SDK Internal Logger internal error while initializing 'FILE': '%s'.", e.toString()));
                }
                return;

            default:
                return;
        }
    }

    private void onInitializationError(String errorMessage) {
        try {
            loggerOutput = null;
            loggingLevel = LoggingLevel.OFF;

            // Notify the user
            new ConsoleLoggerOutput().log(errorMessage);
        } catch (Exception e) {
        }
    }

    private void log(LoggingLevel requestLevel, String message, Object... args) {
        if (requestLevel.getValue() >= loggingLevel.getValue()) {
            loggerOutput.log(createMessage(requestLevel.toString(), message, args));
        }
    }
}
