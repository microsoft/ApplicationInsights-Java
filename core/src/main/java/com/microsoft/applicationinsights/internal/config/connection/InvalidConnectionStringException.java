package com.microsoft.applicationinsights.internal.config.connection;

public class InvalidConnectionStringException extends Exception {

    InvalidConnectionStringException(String message) {
        super(message);
    }

    InvalidConnectionStringException(String message, Throwable cause) {
        super(message, cause);
    }
}
