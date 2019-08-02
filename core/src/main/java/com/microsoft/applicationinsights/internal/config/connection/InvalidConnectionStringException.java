package com.microsoft.applicationinsights.internal.config.connection;

public class InvalidConnectionStringException extends ConnectionStringParseException {

    InvalidConnectionStringException(String message) {
        super(message);
    }

    public InvalidConnectionStringException(String message, Throwable cause) {
        super(message, cause);
    }
}
