package com.microsoft.applicationinsights.internal.config.connection;

public abstract class ConnectionStringParseException extends Exception {
    public ConnectionStringParseException() {
    }

    public ConnectionStringParseException(String message) {
        super(message);
    }

    public ConnectionStringParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionStringParseException(Throwable cause) {
        super(cause);
    }

    public ConnectionStringParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
