package com.microsoft.applicationinsights.smoketest.exceptions;

public class SmokeTestException extends RuntimeException {

    public SmokeTestException(String message) {
        super(message);
    }

    public SmokeTestException(String message, Throwable cause) {
        super(message, cause);
    }
}
