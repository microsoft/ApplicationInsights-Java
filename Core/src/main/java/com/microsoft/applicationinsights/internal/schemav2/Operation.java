package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

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
     * Initializes a new instance of the class.
     */
    public Operation()
    {
        this.InitializeFields();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getParentId() {
        return this.parentId;
    }

    public void setParentId(String value) {
        this.parentId = value;
    }

    public String getRootId() {
        return this.rootId;
    }

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

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        serializeContent(writer);
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("id", id);
        writer.write("name", name);
        writer.write("parentId", parentId);
        writer.write("rootId", rootId);
    }

    protected void InitializeFields() {
    }
}
