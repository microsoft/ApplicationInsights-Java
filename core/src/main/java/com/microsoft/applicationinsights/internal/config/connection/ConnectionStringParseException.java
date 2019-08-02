package com.microsoft.applicationinsights.internal.config.connection;

public abstract class ConnectionStringParseException extends Exception {

    ConnectionStringParseException(String message) {
        super(message);
    }

    public ConnectionStringParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
