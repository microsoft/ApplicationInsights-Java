package com.microsoft.applicationinsights.internal.config.connection;

public class InvalidConnectionStringException extends ConnectionStringParseException {

    InvalidConnectionStringException(String message) {
        super(message);
    }

    InvalidConnectionStringException(Throwable cause) {
        super(cause);
    }

}
