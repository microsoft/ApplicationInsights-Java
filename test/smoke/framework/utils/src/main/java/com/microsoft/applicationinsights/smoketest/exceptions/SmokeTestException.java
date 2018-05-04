package com.microsoft.applicationinsights.smoketest.exceptions;

public class SmokeTestException extends RuntimeException {
    public SmokeTestException() {
        super();
    }

    public SmokeTestException(String message) {
        super(message);
    }

    public SmokeTestException(String message, Throwable cause) {
        super(message, cause);
    }

    public SmokeTestException(Throwable cause) {
        super(cause);
    }

    protected SmokeTestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
