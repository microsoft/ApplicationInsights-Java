package com.microsoft.applicationinsights.channel;

import java.util.HashMap;

/**
 * This interface will provide context for the telemetry channel
 */
public interface ITelemetryContext {
    public HashMap<String, String> toHashMap();
    public HashMap<String, String> getProperties();
}
