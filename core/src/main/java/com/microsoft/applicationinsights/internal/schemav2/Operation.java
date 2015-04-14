/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
