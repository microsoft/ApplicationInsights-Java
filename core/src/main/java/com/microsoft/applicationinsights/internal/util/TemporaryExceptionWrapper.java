package com.microsoft.applicationinsights.internal.util;
import org.slf4j.Logger;

public class TemporaryExceptionWrapper {
    private long successCounter;
    private long failureCounter;
    private Exception lastTemporaryException;
    private Logger lastTemporaryExceptionLogger;
    private String lastTemporaryExceptionMessage;

    public TemporaryExceptionWrapper() {
        this.successCounter = 0L;
        this.failureCounter = 0L;
    }

    public TemporaryExceptionWrapper(long successCounter, long failureCounter, Exception lastTemporaryException, Logger lastTemporaryExceptionLogger, String lastTemporaryExceptionMessage) {
        this.successCounter = successCounter;
        this.failureCounter = failureCounter;
        this.lastTemporaryException = lastTemporaryException;
        this.lastTemporaryExceptionLogger = lastTemporaryExceptionLogger;
        this.lastTemporaryExceptionMessage = lastTemporaryExceptionMessage;
    }

    public long getSuccessCounter() {
        return successCounter;
    }

    public long getFailureCounter() {
        return failureCounter;
    }

    public Exception getLastTemporaryException() {
        return lastTemporaryException;
    }

    public Logger getLastTemporaryExceptionLogger() {
        return lastTemporaryExceptionLogger;
    }

    public String getLastTemporaryExceptionMessage() {
        return lastTemporaryExceptionMessage;
    }

    // only used in tests
    public void incrementSuccessCounter() {
        ++this.successCounter;
    }

    //only used in tests
    public void incrementFailureCounter() {
        ++this.failureCounter;
    }
}
