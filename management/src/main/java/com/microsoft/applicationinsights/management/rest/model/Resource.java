package com.microsoft.applicationinsights.management.rest.model;

import net.minidev.json.JSONObject;

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
    public static Resource fromJSONObject(JSONObject resourceJson) {
        Resource c = new Resource();
        c.setId((String) resourceJson.get("id"));
        c.setName((String) resourceJson.get("name"));
        c.setType((String) resourceJson.get("type"));
        c.setLocation((String) resourceJson.get("location"));

        // TODO: add tags here

        Map<String, String> properties = new HashMap<String, String>();
        JSONObject jsonProperties = (JSONObject) resourceJson.get("properties");
        for (String key : jsonProperties.keySet()) {
            Object value = jsonProperties.get(key);
            if (value instanceof String) {
                properties.put(key, (String) value);
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
