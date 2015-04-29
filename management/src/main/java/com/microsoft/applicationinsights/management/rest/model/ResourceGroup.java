/*
 *
 *  * ApplicationInsights-Java
 *  * Copyright (c) Microsoft Corporation
 *  * All rights reserved.
 *  *
 *  * MIT License
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 *  * software and associated documentation files (the ""Software""), to deal in the Software
 *  * without restriction, including without limitation the rights to use, copy, modify, merge,
 *  * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 *  * persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * The above copyright notice and this permission notice shall be included in all copies or
 *  * substantial portions of the Software.
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 *  * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 *  * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 *  * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 *  * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 *  * DEALINGS IN THE SOFTWARE.
 *
 */

package com.microsoft.applicationinsights.management.rest.model;

import net.minidev.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yonisha on 4/28/2015.
 */
public class ResourceGroup {
    private Subscription subscription;
    private String id;
    private String name;
    private String location;
    private Map<String, String> properties;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return getName();
    }

    public static ResourceGroup fromJSONObject(JSONObject json) {
        ResourceGroup rg = new ResourceGroup();
        rg.setId((String) json.get("id"));
        rg.setName((String) json.get("name"));
        rg.setLocation((String) json.get("location"));

        Map<String, String> properties = new HashMap<String, String>();
        JSONObject jsonProperties = (JSONObject) json.get("properties");
        for (String key : jsonProperties.keySet()) {
            Object value = jsonProperties.get(key);
            if (value instanceof String) {
                properties.put(key, (String) value);
            }
        }

        rg.setProperties(properties);

        return rg;
    }
}

