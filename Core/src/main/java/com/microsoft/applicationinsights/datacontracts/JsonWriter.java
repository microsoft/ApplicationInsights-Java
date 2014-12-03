package com.microsoft.applicationinsights.datacontracts;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Encapsulates logic for serializing objects to JSON.
 */
public interface JsonWriter
{
    /**
     *  Writes opening/left square bracket.
     */
    void writeStartArray()  throws IOException;

    /**
     *  Writes opening/left curly brace.
     */
    void writeStartObject()  throws IOException;

    /**
     *  Writes closing/right square bracket.
     */
    void writeEndArray()  throws IOException;

    /**
     *  Writes closing/right curly brace.
     */
    void writeEndObject()  throws IOException;

    /**
     *  Writes comma.
     */
    void writeComma()  throws IOException;

    /**
     *  Writes a String property.
     */
    void writeProperty(String name, String value)  throws IOException;

    /**
     *  Writes a Boolean property.
     */
    void writeProperty(String name, Boolean value)  throws IOException;

    /**
     *  Writes a Int32 property.
     */
    void writeProperty(String name, Integer value)  throws IOException;

    /**
     *  Writes a Double property.
     */
    void writeProperty(String name, Double value)  throws IOException;

    /**
     *  Writes a Date property.
     */
    void writeProperty(String name, Date value)  throws IOException;

    /**
     *  Writes a Map<String, Double> property.
     */
    void writeMetricsProperty(String name, Map<String, Double> values)  throws IOException;

    /**
     *  Writes a Map<String, String> property.
     */
    void writeProperty(String name, Map<String, String> values)  throws IOException;

    /**
     *  Writes a JsonSerializable object property.
     */
    void writeProperty(String name, JsonSerializable value)  throws IOException;

    /**
     *  Writes a property name in double quotation marks, followed by a colon.
     */
    void writePropertyName(String name)  throws IOException;

    /**
     *  Writes Object as raw value directly.
     */
    void writeRawValue(Object value)  throws IOException;

}
