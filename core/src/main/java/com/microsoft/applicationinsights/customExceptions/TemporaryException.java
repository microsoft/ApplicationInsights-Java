package com.microsoft.applicationinsights.customExceptions;
import org.slf4j.Logger;

public class TemporaryException {
    private Long successCounter;
    private Long failureCounter;
    private Exception lastTemporaryException;
    private Logger lastTemporaryExceptionLogger;
    private String lastTemporaryExceptionMessage;

    public TemporaryException() {
        this.successCounter = 0L;
        this.failureCounter = 0L;
    }

    public TemporaryException(Long successCounter, Long failureCounter, Exception lastTemporaryException, Logger lastTemporaryExceptionLogger, String lastTemporaryExceptionMessage) {
        this.successCounter = successCounter;
        this.failureCounter = failureCounter;
        this.lastTemporaryException = lastTemporaryException;
        this.lastTemporaryExceptionLogger = lastTemporaryExceptionLogger;
        this.lastTemporaryExceptionMessage = lastTemporaryExceptionMessage;
    }

    public Long getSuccessCounter() {
        return successCounter;
    }

    public void setSuccessCounter(Long successCounter) {
        this.successCounter = successCounter;
    }

    public Long getFailureCounter() {
        return failureCounter;
    }

    public void setFailureCounter(Long failureCounter) {
        this.failureCounter = failureCounter;
    }

    public Exception getLastTemporaryException() {
        return lastTemporaryException;
    }

    public void setLastTemporaryException(Exception lastTemporaryException) {
        this.lastTemporaryException = lastTemporaryException;
    }

    public Logger getLastTemporaryExceptionLogger() {
        return lastTemporaryExceptionLogger;
    }

    public void setLastTemporaryExceptionLogger(Logger lastTemporaryExceptionLogger) {
        this.lastTemporaryExceptionLogger = lastTemporaryExceptionLogger;
    }

    public String getLastTemporaryExceptionMessage() {
        return lastTemporaryExceptionMessage;
    }

    public void setLastTemporaryExceptionMessage(String lastTemporaryExceptionMessage) {
        this.lastTemporaryExceptionMessage = lastTemporaryExceptionMessage;
    }

    public void incrementSuccessCounter() {
        ++this.successCounter;
    }

    public void incrementFailureCounter() {
        ++this.failureCounter;
    }
}
