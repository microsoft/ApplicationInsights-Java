package com.microsoft.applicationinsights.agent.bootstrap.customExceptions;

// This class is duplicated from com.microsoft.applicationinsights.customExceptions.FriendlyException since we are not able to import core packages to agent-bootstrap
public class FriendlyException extends RuntimeException {
    public FriendlyException() {
        super();
    }

    public FriendlyException(String banner, String message, String action, String note, Throwable cause) {
        super(populateFriendlyMessage(banner, message, action, note), cause);
    }

    public FriendlyException(String banner, String message, String action, String note) {
        super(populateFriendlyMessage(banner, message, action, note));
    }

    public FriendlyException(String banner, String message, String action) {
        super(populateFriendlyMessage(banner, message, action, ""));
    }

    public FriendlyException(String banner, String action, Throwable cause) {
        super(populateFriendlyMessage(banner, "", action, ""), cause);
    }

    private static String populateFriendlyMessage(String banner, String description, String action, String note) {
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
        if (!note.isEmpty()) {
            messageBuilder.append(System.lineSeparator());
            messageBuilder.append("Note:").append(System.lineSeparator());
            messageBuilder.append(note).append(System.lineSeparator());
        }
        return new String(messageBuilder);
    }
}