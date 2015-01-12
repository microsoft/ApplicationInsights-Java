package com.microsoft.applicationinsights.telemetry;

import java.io.IOException;

/**
 *  Represents objects that support serialization to JSON.
 */
public interface JsonSerializable
{
    /**
     *  Writes JSON representation of the object to the specified writer.
     */
    void serialize(JsonTelemetryDataSerializer serializer) throws IOException;
}
