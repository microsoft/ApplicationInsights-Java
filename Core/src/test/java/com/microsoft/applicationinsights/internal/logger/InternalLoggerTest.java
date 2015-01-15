package com.microsoft.applicationinsights.internal.logger;

import org.junit.Test;

import static org.junit.Assert.*;

public class InternalLoggerTest {
    private static final class StubLoggerOutput implements LoggerOutput {
        private String message;

        @Override
        public void log(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}