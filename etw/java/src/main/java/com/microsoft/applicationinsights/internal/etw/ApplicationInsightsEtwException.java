package com.microsoft.applicationinsights.internal.etw;

public class ApplicationInsightsEtwException extends Exception {
    private static final long serialVersionUID = 6108441736100165651L;
    public ApplicationInsightsEtwException() {
        super();
    }

    public ApplicationInsightsEtwException(String message) {
        super(message);
    }

    public ApplicationInsightsEtwException(String message, Throwable cause) {
        super(message, cause);
    }
}