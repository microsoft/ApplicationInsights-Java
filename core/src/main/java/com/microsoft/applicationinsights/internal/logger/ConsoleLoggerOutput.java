package com.microsoft.applicationinsights.internal.logger;

/**
 * Created by gupele on 1/14/2015.
 */
public final class ConsoleLoggerOutput implements LoggerOutput {
    @Override
    public void log(String message) {
        System.err.println(message);
    }
}
