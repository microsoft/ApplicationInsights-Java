package com.microsoft.applicationinsights.agent.bootstrap.customExceptions;

// This class is duplicated from com.microsoft.applicationinsights.customExceptions.FriendlyException since we are not able to import core packages to agent-bootstrap
public class FriendlyException extends RuntimeException {

    public FriendlyException() {
        super();
    }

    public FriendlyException(String message, String action) {
        super(populateFriendlyMessage(message, action));
    }

    public String getMessageWithBanner(String banner) {
        return new StringBuilder()
                .append(System.lineSeparator())
                .append("*************************")
                .append(System.lineSeparator())
                .append(banner)
                .append(System.lineSeparator())
                .append("*************************")
                .append(getMessage()) // getMessage() is prefixed with lineSeparator already
                .toString();
    }

    private static String populateFriendlyMessage(String description, String action) {
        return new StringBuilder()
                .append(System.lineSeparator())
                .append("Description:")
                .append(System.lineSeparator())
                .append(description)
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("Action:")
                .append(System.lineSeparator())
                .append(action)
                .append(System.lineSeparator())
                .toString();
    }
}