package com.microsoft.applicationinsights.customExceptions;

public class FriendlyException extends RuntimeException {
    public FriendlyException() {
        super();
    }

    public FriendlyException(String message, String action) {
        // TODO can these constructors cascade?
        super(populateFriendlyMessage(message, action));
    }

    public FriendlyException(String banner, String message, String action, String note, Throwable cause) {
        super(populateFriendlyMessage(banner, message, action, note), cause);
    }

    public FriendlyException(String banner, String message, String action, String note) {
        super(populateFriendlyMessage(banner, message, action, note));
    }

    public FriendlyException(String banner, String action, Throwable cause) {
        super(populateFriendlyMessage(banner, "", action, ""), cause);
    }

    // TODO consolidate with method below?
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

    // TODO consolidate with method below
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
        return messageBuilder.toString();
    }
}