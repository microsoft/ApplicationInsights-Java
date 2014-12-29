package com.microsoft.applicationinsights.implementation.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class Operation.
 */
public class Operation implements JsonSerializable {
    /**
     * Backing field for property Id.
     */
    private String id;

    /**
     * Backing field for property Name.
     */
    private String name;

    /**
     * Backing field for property ParentId.
     */
    private String parentId;

    /**
     * Backing field for property RootId.
     */
    private String rootId;

    /**
     * Initializes a new instance of the <see cref="Operation"/> class.
     */
    public Operation()
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
     * Gets the Name property.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the Name property.
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the ParentId property.
     */
    public String getParentId() {
        return this.parentId;
    }

    /**
     * Sets the ParentId property.
     */
    public void setParentId(String value) {
        this.parentId = value;
    }

    /**
     * Gets the RootId property.
     */
    public String getRootId() {
        return this.rootId;
    }

    /**
     * Sets the RootId property.
     */
    public void setRootId(String value) {
        this.rootId = value;
    }


    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
    public void addToHashMap(HashMap<String, String> map)
    {
        if (this.id == null) {
            map.put("id", this.id);
        }
        if (this.name == null) {
            map.put("name", this.name);
        }
        if (this.parentId == null) {
            map.put("parentId", this.parentId);
        }
        if (this.rootId == null) {
            map.put("rootId", this.rootId);
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
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("id", id);
        writer.write("name", name);
        writer.write("parentId", parentId);
        writer.write("rootId", rootId);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {

    }
}
