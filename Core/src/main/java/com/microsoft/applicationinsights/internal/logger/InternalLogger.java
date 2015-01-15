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
     * @param message
     * @param args
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
