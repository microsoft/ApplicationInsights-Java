package com.microsoft.applicationinsights.telemetry;

import java.io.IOException;

/**
 *  Represents objects that support serialization to JSON.
 */
public interface JsonSerializable
{
    /**
     *  Writes JSON representation of the object to the specified writer.
     * @param serializer The {@link com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer} that is used to serialized to JSON format
     * @throws IOException The exception that might be thrown during serialization.
     */
    void serialize(JsonTelemetryDataSerializer serializer) throws IOException;
}
