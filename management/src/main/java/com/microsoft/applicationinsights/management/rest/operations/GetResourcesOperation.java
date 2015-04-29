package com.microsoft.applicationinsights.management.rest.operations;

import com.microsoft.applicationinsights.management.rest.client.HttpMethod;
import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

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

    public List<Resource> execute(Client restClient) throws IOException, AzureCmdException {
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

        JSONObject json = (JSONObject) JSONValue.parse(resultJson);
        JSONArray resourcesArray = (JSONArray) json.get("value");

        for (Object o : resourcesArray) {
            if (o instanceof JSONObject) {
                JSONObject resourceJson = (JSONObject) o;
                Resource c = Resource.fromJSONObject(resourceJson);

                resources.add(c);
            }
        }

        return resources;
    }
}
