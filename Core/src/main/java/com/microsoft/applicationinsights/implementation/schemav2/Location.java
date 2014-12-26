package com.microsoft.applicationinsights.implementation.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

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
     * Initializes a new instance of the <see cref="Location"/> class.
     */
    public Location()
    {
        this.InitializeFields();
    }

    /**
     * Gets the Ip property.
     */
    public String getIp() {
        return this.ip;
    }

    /**
     * Sets the Ip property.
     */
    public void setIp(String value) {
        this.ip = value;
    }


    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
    public void addToHashMap(HashMap<String, String> map)
    {
        if (!(this.ip == null)) {
            map.put("ip", this.ip);
        }
    }


    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        serializeContent(writer);
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException
    {
        writer.write("ip", ip);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {

    }
}
