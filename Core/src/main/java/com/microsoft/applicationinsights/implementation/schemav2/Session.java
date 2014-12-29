package com.microsoft.applicationinsights.implementation.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

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
     * Initializes a new instance of the <see cref="Session"/> class.
     */
    public Session()
    {
        this.InitializeFields();
    }

    /**
     * Gets the Id property.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Sets the Id property.
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the IsFirst property.
     */
    public String getIsFirst() {
        return this.isFirst;
    }

    /**
     * Sets the IsFirst property.
     */
    public void setIsFirst(String value) {
        this.isFirst = value;
    }

    /**
     * Gets the IsNew property.
     */
    public String getIsNew() {
        return this.isNew;
    }

    /**
     * Sets the IsNew property.
     */
    public void setIsNew(String value) {
        this.isNew = value;
    }


    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
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
        writer.write("id", id);
        writer.write("isFirst", isFirst);
        writer.write("isNew", isNew);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }
}
