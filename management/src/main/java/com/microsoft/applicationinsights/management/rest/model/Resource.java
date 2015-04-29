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

package com.microsoft.applicationinsights.management.rest.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yonisha on 4/19/2015.
 *
 * A class represents a resource in Azure.
 */
public class Resource {
    private String id;
    private String name;
    private String type;
    private String location;
    private List<String> tags;
    private Map<String, String> properties;

    /**
     * Gets the resource ID.
     * @return The resource ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the resource ID.
     * @param id The resource ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the resource name.
     * @return The resource name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the resource name.
     * @param name The resource name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the resource type.
     * @return The resource type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the resource type.
     * @param type The resource type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the resource location.
     * @return The resource location.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the resource location.
     * @param location The resource location.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the resource tags.
     * @return The resource tags.
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Sets the resource tags.
     * @param tags The resource tags.
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the resource properties.
     * @return The resource properties.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Sets the resource properties.
     * @param properties The resource properties.
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     *
     * @param resourceJson Converts the given resource JSON object to Resource object.
     * @return Resource object.
     */
    public static Resource fromJSONObject(JsonObject resourceJson) {
        Resource c = new Resource();
        c.setId(resourceJson.get("id").toString());
        c.setName(resourceJson.get("name").toString());
        c.setType(resourceJson.get("type").toString());
        c.setLocation(resourceJson.get("location").toString());

        // TODO: add tags here

        Map<String, String> properties = new HashMap<String, String>();
        JsonObject jsonProperties = (JsonObject) resourceJson.get("properties");
        for (Map.Entry<String, JsonElement> key : jsonProperties.entrySet()) {
            if (!key.getValue().isJsonPrimitive()) {
                properties.put(key.getKey(), key.getValue().toString());
            }
        }
        c.setProperties(properties);

        return c;
    }

    @Override
    public String toString() {
        return getName();
    }
}
