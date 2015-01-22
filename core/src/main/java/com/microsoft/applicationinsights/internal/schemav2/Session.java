package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class Session.
 */
public class Session implements JsonSerializable {
    /**
     * Backing field for property Id.
     */
    private String id;

    /**
     * Backing field for property IsFirst.
     */
    private String isFirst;

    /**
     * Backing field for property IsNew.
     */
    private String isNew;

    /**
     * Initializes a new instance of the class.
     */
    public Session()
    {
        this.InitializeFields();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getIsFirst() {
        return this.isFirst;
    }

    public void setIsFirst(String value) {
        this.isFirst = value;
    }

    public String getIsNew() {
        return this.isNew;
    }

    public void setIsNew(String value) {
        this.isNew = value;
    }

    public void addToHashMap(HashMap<String, String> map)
    {
        if (!(this.id == null)) {
            map.put("id", this.id);
        }
        if (!(this.isFirst == null)) {
            map.put("isFirst", this.isFirst);
        }
        if (!(this.isNew == null)) {
            map.put("isNew", this.isNew);
        }
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be thrown during serialization.
     */
    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        this.serializeContent(writer);
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("id", id);
        writer.write("isFirst", isFirst);
        writer.write("isNew", isNew);
    }

    protected void InitializeFields() {
    }
}
