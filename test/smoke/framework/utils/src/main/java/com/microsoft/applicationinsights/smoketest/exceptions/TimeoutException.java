package com.microsoft.applicationinsights.smoketest.exceptions;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

public class TimeoutException extends SmokeTestException {
    public TimeoutException(String componentName, long timeout, TimeUnit unit) {
        this(componentName, timeout, unit, "", null);
    }
    public TimeoutException(String componentName, long timeout, TimeUnit unit, String message) {
        this(componentName, timeout, unit, message, null);
    }
    public TimeoutException(String componentName, long timeout, TimeUnit unit, Throwable cause) {
        this(componentName, timeout, unit, "", cause);
    }

    public TimeoutException(String componentName, long timeout, TimeUnit unit, String message, Throwable cause) {
        super(String.format("Timeout reached (%d %s) waiting for %s. %s", timeout, unit.toString().toLowerCase(), componentName, message), cause);
    }
}
