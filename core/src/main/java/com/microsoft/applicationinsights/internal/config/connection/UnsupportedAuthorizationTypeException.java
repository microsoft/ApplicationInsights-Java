package com.microsoft.applicationinsights.internal.config.connection;

public class UnsupportedAuthorizationTypeException extends ConnectionStringParseException {

    UnsupportedAuthorizationTypeException(String message) {
        super(message);
    }

}
