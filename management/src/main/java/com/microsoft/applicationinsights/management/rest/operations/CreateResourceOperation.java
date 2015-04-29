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

package com.microsoft.applicationinsights.management.rest.operations;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.model.Resource;

/**
 * Created by yonisha on 4/20/2015.
 */
public class CreateResourceOperation implements RestOperation<Resource> {

    private final String OPERATION_API_VERSION = "2014-08-01";
    private final String OPERATION_PATH_TEMPLATE =
            "subscriptions/%s/resourceGroups/%s/providers/microsoft.insights/components/%s?api-version=%s";

    private String operationPath;
    private String payload;

    public CreateResourceOperation(String subscriptionId, String resourceGroupName, String resourceName) {
        operationPath = String.format(OPERATION_PATH_TEMPLATE, subscriptionId, resourceGroupName, resourceName, OPERATION_API_VERSION);
        payload = generatePayload();
    }

    public Resource execute(Client restClient) throws IOException, RestOperationException {
        String resourceJson = restClient.executePut(operationPath, payload, OPERATION_API_VERSION);
        Resource resource = parseResult(resourceJson);

        return resource;
    }

    private Resource parseResult(String resultJson) {
        if (resultJson == null || resultJson.isEmpty()) {
            return null;
        }

        JsonObject jsonObj = new JsonParser().parse(resultJson).getAsJsonObject();
        Resource resource = Resource.fromJSONObject(jsonObj);

        return resource;
    }

    private String generatePayload() {
        JsonObject properties = new JsonObject();
        properties.addProperty("Application_Type", "java");
        properties.addProperty("Flow_Type", "Greenfield");
        properties.addProperty("Request_Source", "AIEclipsePlugin");
        JsonObject payload = new JsonObject();

        // TODO: location as a parameter.
        payload.addProperty("location", "centralus");
        payload.add("properties", properties);

        return payload.toString();
    }
}
