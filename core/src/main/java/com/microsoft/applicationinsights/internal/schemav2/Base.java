package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class Base.
 */
public abstract class Base implements JsonSerializable, SendableData{
    /**
     * Backing field for property BaseType.
     */
    private String baseType;

    /**
     * Initializes a new instance of the class.
     */
    public Base() {
        this.InitializeFields();
    }

    public String getBaseType() {
        return this.baseType;
    }

    public void setBaseType(String baseType) {
        this.baseType = baseType;
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        this.serializeContent(writer);
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be throw during serialization.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("baseType", baseType);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }
}
