package com.microsoft.applicationinsights.internal.config.connection;

public abstract class ConnectionStringParseException extends Exception {

    ConnectionStringParseException() {
        super();
    }

    ConnectionStringParseException(String message) {
        super(message);
    }

    ConnectionStringParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
