package com.microsoft.applicationinsights.internal.util;

public class NoSupportedProtocolsException extends RuntimeException {
    public NoSupportedProtocolsException() {
    }

    public NoSupportedProtocolsException(String message) {
        super(message);
    }
}
