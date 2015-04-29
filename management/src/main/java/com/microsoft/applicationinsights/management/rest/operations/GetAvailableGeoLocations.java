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

package com.microsoft.applicationinsights.management.rest.operations;

import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.model.ResourceGroup;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// TODO: implement

/**
 * Created by yonisha on 4/28/2015.
 */
public class GetAvailableGeoLocations implements RestOperation<List<String>> {

    private final String OPERATION_API_VERSION = "2015-01-01";
    private final String OPERATION_PATH_TEMPLATE = "providers/microsoft.insights?api-version=%s";

    private String operationPath;

    public GetAvailableGeoLocations() {
        this.operationPath = String.format(OPERATION_PATH_TEMPLATE, OPERATION_API_VERSION);
    }

    @Override
    public List<String> execute(Client restClient) throws IOException, AzureCmdException {
        String resourceJson = restClient.executeGet(operationPath, OPERATION_API_VERSION);

        List<String> locations = parseResult(resourceJson);

        return locations;
    }

    private List<String> parseResult(String resultJson) {
        List<String> locations = new ArrayList<String>();

        JSONObject json = (JSONObject) JSONValue.parse(resultJson);
        JSONArray jsonResourceTypes = (JSONArray) json.get("resourceTypes");
        Object obj = jsonResourceTypes.get(0);
        if (obj instanceof JSONObject) {
            JSONObject jsonResourceType = (JSONObject) obj ;
            JSONArray jsonLocations = (JSONArray) jsonResourceType.get("locations");

            for (Object location : jsonLocations) {
                locations.add(location.toString());
            }
        }

        return locations;
    }
}
