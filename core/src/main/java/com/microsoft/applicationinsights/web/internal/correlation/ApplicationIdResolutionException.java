package com.microsoft.applicationinsights.web.internal.correlation;

public class ApplicationIdResolutionException extends Exception {
    public ApplicationIdResolutionException() {
    }

    public ApplicationIdResolutionException(String message) {
        super(message);
    }

    public ApplicationIdResolutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationIdResolutionException(Throwable cause) {
        super(cause);
    }
}
