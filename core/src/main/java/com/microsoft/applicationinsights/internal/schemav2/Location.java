package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class Location.
 */
public class Location implements JsonSerializable {
    /**
     * Backing field for property Ip.
     */
    private String ip;

    /**
     * Initializes a new instance of the class.
     */
    public Location()
    {
        this.InitializeFields();
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String value) {
        this.ip = value;
    }

    public void addToHashMap(HashMap<String, String> map)
    {
        if (!(this.ip == null)) {
            map.put("ip", this.ip);
        }
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        serializeContent(writer);
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("ip", ip);
    }

    protected void InitializeFields() {
    }
}
