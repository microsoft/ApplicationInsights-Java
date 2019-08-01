package com.microsoft.applicationinsights.internal.config.connection;

public class InvalidConnectionStringException extends ConnectionStringParseException {
    public InvalidConnectionStringException() {
    }

    public InvalidConnectionStringException(String message) {
        super(message);
    }

    public InvalidConnectionStringException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidConnectionStringException(Throwable cause) {
        super(cause);
    }

    public InvalidConnectionStringException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
