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
import com.microsoft.applicationinsights.management.common.Logger;
import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.model.Subscription;
import com.microsoft.applicationinsights.management.rest.model.Tenant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yonisha on 6/9/2015.
 *
 * API Documentation: https://msdn.microsoft.com/en-us/library/azure/dn790542.aspx
 */
public class GetTenantsOperation implements RestOperation<List<Tenant>> {

    private static final Logger LOG = Logger.getLogger(GetTenantsOperation.class.toString());
    private final String OPERATION_API_VERSION = "2015-01-01";
    private final String OPERATION_PATH_TEMPLATE = "tenants?api-version=%s";
    private Tenant commonTenant;

    public GetTenantsOperation(Tenant commonTenant) {
        this.commonTenant = commonTenant;
    }

    @Override
    public List<Tenant> execute(Client restClient) throws IOException, RestOperationException {
        String operationPath = String.format(OPERATION_PATH_TEMPLATE, OPERATION_API_VERSION);

        LOG.info("Getting available tenants.\nURL Path: {0}.", operationPath);

        String tenantsJson = restClient.executeGet(this.commonTenant, operationPath, OPERATION_API_VERSION);
        List<Tenant> subscriptions = parseResult(tenantsJson);

        return subscriptions;
    }

    private List<Tenant> parseResult(String tenantsJson) {
        List<Tenant> tenants = new ArrayList<Tenant>();

        if (tenantsJson == null || tenantsJson.isEmpty()) {
            return tenants;
        }

        JsonObject json = new JsonParser().parse(tenantsJson).getAsJsonObject();

        JsonArray tenantsProtos = json.getAsJsonArray("value");
        for (int i = 0; i < tenantsProtos.size(); i++) {
            JsonObject tenantJson = tenantsProtos.get(i).getAsJsonObject();
            Tenant tenant = Tenant.fromJSONObject(tenantJson);

            tenants.add(tenant);
        }

        return tenants;
    }
}
