package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

public class Internal implements JsonSerializable {
    /**
     * Backing field for property SdkVersion.
     */
    private String sdkVersion;

    /**
     * Backing field for property AgentVersion.
     */
    private String agentVersion;

    /**
     * Initializes a new instance of the class.
     */
    public Internal()
    {
        this.InitializeFields();
    }

    public String getSdkVersion() {
        return this.sdkVersion;
    }

    public void setSdkVersion(String value) {
        this.sdkVersion = value;
    }

    public String getAgentVersion() {
        return this.agentVersion;
    }

    public void setAgentVersion(String value) {
        this.agentVersion = value;
    }


    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
    public void addToHashMap(HashMap<String, String> map)
    {
        if (this.sdkVersion != null) {
            map.put("sdkVersion", this.sdkVersion);
        }
        if (this.agentVersion != null) {
            map.put("agentVersion", this.agentVersion);
        }
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be throw during serialization.
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
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException
    {
        writer.write("sdkVersion", sdkVersion);
        writer.write("agentVersion", agentVersion);
    }

    protected void InitializeFields() {
    }
}
