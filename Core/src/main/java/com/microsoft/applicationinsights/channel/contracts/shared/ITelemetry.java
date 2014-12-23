package com.microsoft.applicationinsights.channel.contracts.shared;

import java.util.HashMap;

public interface ITelemetry extends ITelemetryData {
    /**
     * Gets the properties.
     */
    public HashMap<String, String> getProperties();

	/**
     * Sets the properties.
     */
    public void setProperties(HashMap<String, String> value);
}
