package com.microsoft.gcmonitor;

public class UnableToMonitorMemoryException extends Exception {

    public UnableToMonitorMemoryException(Exception cause) {
        super(cause);
    }

    public UnableToMonitorMemoryException(String message, Exception cause) {
        super(message, cause);
    }

    public UnableToMonitorMemoryException(String message) {
        super(message);
    }
}
