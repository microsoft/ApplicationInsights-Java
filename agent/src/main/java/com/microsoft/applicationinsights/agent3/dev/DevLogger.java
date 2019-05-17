package com.microsoft.applicationinsights.agent3.dev;

public class DevLogger {
    private final String className;
    public DevLogger(Class<?> clazz) {
        this.className = clazz.getSimpleName();
    }

    public void info(String message) {
        System.out.println("["+className+"] "+message);
    }

    public void info(String message, Throwable t) {
        info(message + " " + t.getLocalizedMessage());
    }

    public void info(String message, Object...args) {
        info(String.format(message, args));
    }

    public void info(String message, Throwable t, Object...args) {
        info(String.format(message, args), t);
    }

}
