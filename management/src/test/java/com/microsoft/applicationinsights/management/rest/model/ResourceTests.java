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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by yonisha on 6/15/2015.
 */
public class ResourceTests {
    private final String DEFAULT_RESOURCE_GROUP = "resource_group";
    private final String DEFAULT_RESOURCE_NAME = "resource_name";
    private final String DEFAULT_LOCATION = "central us";
    private final String DEFAULT_RESOURCE_TYPE = "microsoft.insights";
    private final String DEFAULT_IKey = "Ikey";
    private final String RESOURCE_ID_TEMPLATE = "/subscriptions/some_id/resourceGroups/%s/providers/%s/components/%s";

    @Test
    public void testFromJSONObject() {
        String resourceId = String.format(RESOURCE_ID_TEMPLATE, DEFAULT_RESOURCE_GROUP, DEFAULT_RESOURCE_TYPE, DEFAULT_RESOURCE_NAME);
        JsonObject defaultResourceJsonObject = createDefaultResourceJsonObject(resourceId);

        Resource resource = Resource.fromJSONObject(defaultResourceJsonObject);

        Assert.assertEquals(DEFAULT_RESOURCE_NAME, resource.getName());
        Assert.assertEquals(resourceId, resource.getId());
        Assert.assertEquals(DEFAULT_RESOURCE_GROUP, resource.getResourceGroup());
        Assert.assertEquals(DEFAULT_LOCATION, resource.getLocation());
        Assert.assertEquals(DEFAULT_RESOURCE_TYPE, resource.getType());
        Assert.assertEquals(DEFAULT_IKey, resource.getInstrumentationKey());
    }

    private JsonObject createDefaultResourceJsonObject(String resourceId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("id", new JsonPrimitive(resourceId));
        jsonObject.add("name", new JsonPrimitive(DEFAULT_RESOURCE_NAME));
        jsonObject.add("type", new JsonPrimitive(DEFAULT_RESOURCE_TYPE));
        jsonObject.add("location", new JsonPrimitive(DEFAULT_LOCATION));

        JsonObject propertiesJsonObject = new JsonObject();
        propertiesJsonObject.add("InstrumentationKey", new JsonPrimitive(DEFAULT_IKey));

        jsonObject.add("properties", propertiesJsonObject);

        return jsonObject;
    }
}