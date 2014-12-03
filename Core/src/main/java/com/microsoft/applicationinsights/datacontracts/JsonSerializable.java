package com.microsoft.applicationinsights.datacontracts;

import java.io.IOException;

/**
 *  Represents objects that support serialization to JSON.
 */
public interface JsonSerializable
{
    /**
     *  Writes JSON representation of the object to the specified writer.
     */
    void serialize(JsonWriter writer) throws IOException;
}
