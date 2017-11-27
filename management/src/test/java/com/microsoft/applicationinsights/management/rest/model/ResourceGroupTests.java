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
 * Created by yonisha on 6/16/2015.
 */
public class ResourceGroupTests {

    private final String DEFAULT_ID = "id";
    private final String DEFAULT_RESOURCE_GROUP_NAME = "resource_group_name";
    private final String DEFAULT_LOCATION = "location";
    private final String DEFAULT_PROPERTY_KEY = "prop_key";
    private final String DEFAULT_PROPERTY_VALUE = "prop_value";

    @Test
    public void testFromJSONObject() {
        JsonObject resourceGroupJsonObject = createDefaultResourceGroupJsonObject();

        ResourceGroup resourceGroup = ResourceGroup.fromJSONObject(resourceGroupJsonObject);

        Assert.assertEquals(DEFAULT_ID, resourceGroup.getId());
        Assert.assertEquals(DEFAULT_RESOURCE_GROUP_NAME, resourceGroup.getName());
        Assert.assertEquals(DEFAULT_LOCATION, resourceGroup.getLocation());
        Assert.assertEquals(DEFAULT_PROPERTY_VALUE, resourceGroup.getProperties().get(DEFAULT_PROPERTY_KEY));
    }

    private JsonObject createDefaultResourceGroupJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("id", new JsonPrimitive(DEFAULT_ID));
        jsonObject.add("name", new JsonPrimitive(DEFAULT_RESOURCE_GROUP_NAME));
        jsonObject.add("location", new JsonPrimitive(DEFAULT_LOCATION));

        JsonObject propertiesJsonObject = new JsonObject();
        propertiesJsonObject.add(DEFAULT_PROPERTY_KEY, new JsonPrimitive(DEFAULT_PROPERTY_VALUE));

        jsonObject.add("properties", propertiesJsonObject);

        return jsonObject;
    }
}
