package com.microsoft.applicationinsights.agent.bootstrap.customExceptions;

public class FriendlyException extends Exception {
    public FriendlyException() {
        super();
    }

    public FriendlyException(String banner, String message, String action, Throwable cause) {
        super(populateFriendlyMessage(banner, message, action), cause);
    }

    public FriendlyException(String banner, String message, String action) {
        super(populateFriendlyMessage(banner, message, action));
    }

    public FriendlyException(String banner, String action, Throwable cause) {
        super(populateFriendlyMessage(banner, "",action), cause);
    }

    private static String populateFriendlyMessage(String banner, String description, String action) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(System.lineSeparator());
        messageBuilder.append("*************************").append(System.lineSeparator());
        messageBuilder.append(banner).append(System.lineSeparator());
        messageBuilder.append("*************************").append(System.lineSeparator());
        messageBuilder.append(System.lineSeparator());
        messageBuilder.append("Description:").append(System.lineSeparator());
        messageBuilder.append(description).append(System.lineSeparator());
        messageBuilder.append(System.lineSeparator());
        messageBuilder.append("Action:").append(System.lineSeparator());
        messageBuilder.append(action).append(System.lineSeparator());
        return new String(messageBuilder);
    }
}
