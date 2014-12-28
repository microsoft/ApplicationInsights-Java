package com.microsoft.applicationinsights.implementation.schemav2;

import java.io.IOException;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class Domain.
 */
public class Domain implements SendableData
{
    /**
     * Envelope Name for this telemetry.
     */
    private static final String DOMAIN_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Do";

    /**
     * Base Type for this telemetry.
     */
    private static final String DOMAIN_BASE_TYPE = "Microsoft.ApplicationInsights.Domain";

    /**
     * Initializes a new instance of the <see cref="Domain"/> class.
     */
    public Domain() {
        this.InitializeFields();
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
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
    }

    protected void InitializeFields() {
    }

    @Override
    public String getEnvelopName() {
        return DOMAIN_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return DOMAIN_BASE_TYPE;
    }
}
