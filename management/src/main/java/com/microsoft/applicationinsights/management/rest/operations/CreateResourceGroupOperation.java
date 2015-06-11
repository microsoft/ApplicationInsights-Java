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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.applicationinsights.management.common.Logger;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.model.ResourceGroup;
import com.microsoft.applicationinsights.management.rest.model.Tenant;

import java.io.IOException;

/**
 * Created by yonisha on 4/28/2015.
 */
public class CreateResourceGroupOperation implements RestOperation<ResourceGroup> {

    private static final Logger LOG = Logger.getLogger(CreateResourceGroupOperation.class.toString());
    private final String OPERATION_API_VERSION = "2015-01-01";
    private String OPERATION_PATH_TEMPLATE = "subscriptions/%s/resourcegroups/%s?api-version=%s";
    private Tenant tenant;
    private String operationPath;
    private String payload;

    public CreateResourceGroupOperation(Tenant tenant, String subscriptionId, String resourceGroupName, String location) {
        this.operationPath = String.format(OPERATION_PATH_TEMPLATE, subscriptionId, resourceGroupName, OPERATION_API_VERSION);
        this.payload = generatePayload(location);
        this.tenant = tenant;
    }

    @Override
    public ResourceGroup execute(Client restClient) throws IOException, RestOperationException {
        LOG.info("Creating new resource group.\nURL path: {0}\nPayload:{1}", this.operationPath, this.payload);

        String resourceJson = restClient.executePut(tenant, operationPath, payload, OPERATION_API_VERSION);
        ResourceGroup resourceGroup = parseResult(resourceJson);

        return resourceGroup;
    }

    private ResourceGroup parseResult(String resultJson) {
        if (resultJson == null || resultJson.isEmpty()) {
            return null;
        }

        JsonObject jsonObj = new JsonParser().parse(resultJson).getAsJsonObject();
        ResourceGroup resourceGroup = ResourceGroup.fromJSONObject(jsonObj);

        return resourceGroup;
    }

    private String generatePayload(String location) {
        JsonObject payload = new JsonObject();
        payload.addProperty("location", location);

        return payload.toString();
    }
}
