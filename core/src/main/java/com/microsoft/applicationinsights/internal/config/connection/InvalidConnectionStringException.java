package com.microsoft.applicationinsights.internal.config.connection;

public class InvalidConnectionStringException extends ConnectionStringParseException {

    InvalidConnectionStringException() {
        super();
    }

    InvalidConnectionStringException(String message) {
        super(message);
    }

    InvalidConnectionStringException(String message, Throwable cause) {
        super(message, cause);
    }
}
