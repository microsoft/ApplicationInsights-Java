package com.microsoft.applicationinsights.internal.config.connection;

public class UnsupportedAuthorizationTypeException extends ConnectionStringParseException {
    public UnsupportedAuthorizationTypeException() {
    }

    public UnsupportedAuthorizationTypeException(String message) {
        super(message);
    }

    public UnsupportedAuthorizationTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedAuthorizationTypeException(Throwable cause) {
        super(cause);
    }

    public UnsupportedAuthorizationTypeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
