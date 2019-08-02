package com.microsoft.applicationinsights.internal.config.connection;

public abstract class ConnectionStringParseException extends Exception {

    ConnectionStringParseException(String message) {
        super(message);
    }

    ConnectionStringParseException(Throwable cause) {
        super(cause);
    }

}
