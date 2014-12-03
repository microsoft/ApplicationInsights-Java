package com.microsoft.applicationinsights.channel;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.TelemetryContext;

import java.util.Date;
import java.util.Map;

/**
 * The base telemetry type interface for application insights.
 */
public interface Telemetry extends JsonSerializable
{
    /**
     * Gets the time when telemetry was recorded
     */
    Date getTimestamp();
    /**
     * Sets the time when telemetry was recorded
     */
    void setTimestamp(Date date);

    /**
     * Gets the context associated with this telemetry instance.
     */
    TelemetryContext getContext();

    /**
     * Gets the map of application-defined property names and values.
     */
    Map<String,String> getProperties();

    /**
     * Sanitizes the properties of the telemetry item based on DP constraints.
     */
    void sanitize();
}
