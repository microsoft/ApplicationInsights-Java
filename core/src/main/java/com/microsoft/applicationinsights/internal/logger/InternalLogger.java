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

    public enum LoggingLevel {
        ON,
        OFF
    }

    public enum LoggerOutputType {
        CONSOLE
    }

    private boolean initialized = false;

    private LoggingLevel loggingLevel = LoggingLevel.OFF;

    private LoggerOutput loggerOutput = new ConsoleLoggerOutput();

    private InternalLogger() {
    }

    public synchronized void initialize(String loggerOutput, boolean isEnabled) {
        if (!initialized) {
            setLoggerOutput(loggerOutput);
            setEnabled(isEnabled);
            initialized = true;
        }
    }

    public boolean isEnabled() {
        return loggingLevel == LoggingLevel.ON;
    }

    /**
     * The main method, will delegate the call to the output
     * only if the logger is enabled, will not allow any exception thrown
     * @param message The message to log with possible placeholders.
     * @param args The arguments that should be formatted into the placeholders.
     */
    public void log(String message, Object... args) {
        try {
            if (isEnabled()) {
                loggerOutput.log(String.format(message, args));
            }
        } catch (Throwable t) {
        }
    }

    private void setLoggerOutput(String loggerOutputType) {
        if (Strings.isNullOrEmpty(loggerOutputType)) {
            return;
        }

        try {
            LoggerOutputType type = LoggerOutputType.valueOf(loggerOutputType);
            switch (type) {
                case CONSOLE:
                    loggerOutput = new ConsoleLoggerOutput();
                    return;
            }
        } catch (Exception e) {
        }
    }

    private void setEnabled(boolean enabled) {
        loggingLevel = enabled ? LoggingLevel.ON : LoggingLevel.OFF;
    }

}
