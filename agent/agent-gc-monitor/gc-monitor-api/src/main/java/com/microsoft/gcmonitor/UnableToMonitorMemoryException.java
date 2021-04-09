package com.microsoft.gcmonitor;

public class UnableToMonitorMemoryException extends Exception {

    private static final long serialVersionUID = 6087277942238447001L;

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
