package com.microsoft.applicationinsights.internal.schemav2;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;

/**
 * Defines the interface for data that might be sent to the Server.
 */
public interface SendableData extends JsonSerializable {
    String getEnvelopName();

    String getBaseTypeName();
}
