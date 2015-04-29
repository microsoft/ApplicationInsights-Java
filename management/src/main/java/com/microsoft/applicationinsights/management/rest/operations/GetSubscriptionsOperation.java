package com.microsoft.applicationinsights.management.rest.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.applicationinsights.management.rest.client.HttpMethod;
import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.model.Subscription;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * Created by yonisha on 4/19/2015.
 */
public class GetSubscriptionsOperation implements RestOperation<List<Subscription>> {

    // TODO: embbed the service url here!
    private final String OPERATION_API_VERSION = "2014-06-01";
    private final String OPERATION_PATH_TEMPLATE = "subscriptions?api-version=%s";

    public List<Subscription> execute(Client restClient) throws IOException, AzureCmdException {
        String operationPath = String.format(OPERATION_PATH_TEMPLATE, OPERATION_API_VERSION);
        String subscriptionsJson = restClient.executeGet(operationPath, OPERATION_API_VERSION);

        List<Subscription> subscriptions = parseResult(subscriptionsJson);

        return subscriptions;
    }

    private List<Subscription> parseResult(String resultJson) {
        List<Subscription> subscriptions = new ArrayList<Subscription>();

        if (resultJson == null || resultJson.isEmpty()) {
            return subscriptions;
        }

        JSONObject json = (JSONObject) JSONValue.parse(resultJson);

        JSONArray subscriptionProtos = (JSONArray) json.get("value");
        for (int i = 0; i < subscriptionProtos.size(); i++) {
            JSONObject subscriptionJson = (JSONObject) subscriptionProtos.get(i);
            Subscription subscription = Subscription.fromJSONObject(subscriptionJson);

            // TODO: why needed??
            if (!subscriptions.contains(subscription)) {
                subscriptions.add(subscription);
            }
        }

        return subscriptions;
    }
}
