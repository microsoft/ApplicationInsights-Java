package com.microsoft.applicationinsights.management.rest.operations;

import java.io.IOException;
import com.microsoft.applicationinsights.management.rest.client.HttpMethod;
import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

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

    public Resource execute(Client restClient) throws IOException, AzureCmdException {
        String resourceJson = restClient.executePut(operationPath, payload, OPERATION_API_VERSION);
        Resource resource = parseResult(resourceJson);

        return resource;
    }

    private Resource parseResult(String resultJson) {
        if (resultJson == null || resultJson.isEmpty()) {
            return null;
        }

        JSONObject jsonObj = (JSONObject) JSONValue.parse(resultJson);
        Resource resource = Resource.fromJSONObject(jsonObj);

        return resource;
    }

    private String generatePayload() {
        JSONObject properties = new JSONObject();
        properties.put("Application_Type", "java");
        properties.put("Flow_Type", "Greenfield");
        properties.put("Request_Source", "AIEclipsePlugin");
        JSONObject payload = new JSONObject();

        // TODO: location as a parameter.
        payload.put("location", "centralus");
        payload.put("properties", properties);

        return payload.toJSONString();
    }
}
