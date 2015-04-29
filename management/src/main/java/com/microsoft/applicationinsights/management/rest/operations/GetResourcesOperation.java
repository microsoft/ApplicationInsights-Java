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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.model.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yonisha on 4/19/2015.
 */
public class GetResourcesOperation implements RestOperation<List<Resource>> {

    private final String OPERATION_API_VERSION = "2014-08-01";
    private final String OPERATION_PATH_TEMPLATE = "subscriptions/%s/providers/microsoft.insights/components?api-version=%s";

    private String subscriptionId;

    public GetResourcesOperation(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public List<Resource> execute(Client restClient) throws IOException, RestOperationException {
        String operationPath = String.format(OPERATION_PATH_TEMPLATE, subscriptionId, OPERATION_API_VERSION);
        String resourcesJson = restClient.executeGet(operationPath, OPERATION_API_VERSION);

        List<Resource> resources = parseResult(resourcesJson);

        return resources;
    }

    private List<Resource> parseResult(String resultJson) {
        List<Resource> resources = new ArrayList<Resource>();

        if (resultJson == null || resultJson.isEmpty()) {
            return resources;
        }

        JsonObject json = new JsonParser().parse(resultJson).getAsJsonObject();
        JsonArray resourcesArray = json.getAsJsonArray("value");

        for (Object o : resourcesArray) {
            if (o instanceof JsonObject) {
                JsonObject resourceJson = (JsonObject) o;
                Resource c = Resource.fromJSONObject(resourceJson);

                resources.add(c);
            }
        }

        return resources;
    }
}
