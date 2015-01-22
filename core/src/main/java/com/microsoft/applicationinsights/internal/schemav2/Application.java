package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class Application.
 */
public class Application implements JsonSerializable {
    /**
     * Backing field for property Ver.
     */
    private String ver;

    /**
     */
    public Application()
    {
        this.InitializeFields();
    }

    public String getVer() {
        return this.ver;
    }

    public void setVer(String value) {
        this.ver = value;
    }


    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
    public void addToHashMap(HashMap<String, String> map)
    {
        if (this.ver != null) {
            map.put("ver", this.ver);
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

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("ver", ver);
    }

    protected void InitializeFields() {
    }
}
