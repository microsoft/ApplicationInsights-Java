package com.microsoft.applicationinsights.azuretest;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import org.apache.commons.lang3.time.StopWatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

/**
 * Created by moralt on 30/4/2015.
 */
public class CompareTelemetryWithAzureExportTests {
    String runId = UUID.randomUUID().toString();
    String configEnvironmentVariable = "INTEGRATION_TEST_CONFIGURATION";
    IntegrationTestConfiguration config;
    private static final int millisecondsInSecond = 1000;

    /**
     * Sends GET requests to server and expects to get similar result from app insights
     * @throws Exception
     */
    @Test
    public void sendHttpRequestTest() throws Exception {

        initConfiguration();
        clearQueue();

        ArrayList<TelemetryItem> expectedTelemetries = sentHttpGetRequests();
        ArrayList<TelemetryItem> realTelemetries = getTelemetryFromAzure(DocumentType.Requests);

        ArrayList<TelemetryItem> missingTelemetry = getMissingExpectedTelemetry(realTelemetries, expectedTelemetries);
        if (missingTelemetry.size() > 0) {
            for (TelemetryItem item : missingTelemetry) {
                System.out.println("Didn't find matching item in real telemetry for request of URI " + item.getProperty("uri"));
            }

            throw new Exception("Didn't find match for " + missingTelemetry.size() + " items");
        }
        else {
            System.out.println("Test passed successfully");
        }
    }

    private void initConfiguration() throws JAXBException {
        String location = System.getenv(configEnvironmentVariable);
        Map<String, String> env =  System.getenv();
        System.out.println("Configuration file located in " + location);

        try {
           config = IntegrationTestConfiguration.load(location);
        }
        catch(JAXBException e) {
            System.out.println("Failed to load configuration file " + location);
            throw e;
        }
    }

    /**
     * Clears the Azure queue from all existing messages
     * @throws Exception
     */
    private void clearQueue() throws Exception {
        CloudStorageAccount account = CloudStorageAccount.parse(config.getStorageConnectionString());
        CloudQueueClient queueClient = account.createCloudQueueClient();
        CloudQueue queue = queueClient.getQueueReference(config.getQueueName());

        queue.downloadAttributes();
        System.out.println("Before clearing: Queue contains " + queue.getApproximateMessageCount() + " items");
        System.out.println("Clearing queue ...");
        queue.clear();
    }

    private ArrayList<TelemetryItem> sentHttpGetRequests() throws Exception {

        String serverAddress = config.getTestServerAddress();
        int tomcat7Port = config.getTomcat7Port();
        String applicationFollder = config.getApplicationFolder();

        String requestId1 = UUID.randomUUID().toString();
        String requestId2 = UUID.randomUUID().toString();
        String requestId3 = UUID.randomUUID().toString();

        URI uri1 = new URI("http://" + serverAddress + ":" + tomcat7Port + "/" + applicationFollder + "/books?id=Thriller&runId=" + runId + "&requestId=" + requestId1);
        URI uri2 = new URI("http://" + serverAddress + ":" + tomcat7Port + "/" + applicationFollder + "/loan?title=Gone%20Girl&id=030758836x&subject=Thriller&runId=" + runId + "&requestId=" + requestId2);
        URI uri3 = new URI("http://" + serverAddress + ":" + tomcat7Port + "/" + applicationFollder + "/notExistingWebFage?runId=" + runId + "&requestId=" + requestId3);

        TelemetryItem expectedTelemetry1 = sendGet(uri1, requestId1);
        TelemetryItem expectedTelemetry2 = sendGet(uri2, requestId2);
        TelemetryItem expectedTelemetry3 = sendGet(uri3, requestId3);

        ArrayList<TelemetryItem> expectedTelemetries = new ArrayList<TelemetryItem>();
        expectedTelemetries.add(expectedTelemetry1);
        expectedTelemetries.add(expectedTelemetry2);
        expectedTelemetries.add(expectedTelemetry3);

        return expectedTelemetries;
    }

    /**
     * Sends HTTP GET request and returns the expected telemetry from AppInsights
     * @param uri The URI for the request
     * @return The expected telemetry items from AppInsights
     * @throws Exception
     */
    private TelemetryItem sendGet(URI uri, String requestId) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

        // optional default is GET
        connection.setRequestMethod("GET");

        //add request header
        connection.setRequestProperty("User-Agent", config.getUserAgent());

        System.out.println("Sending 'GET' request to URL: " + uri.toString());
        Integer responseCode = connection.getResponseCode();
        System.out.println("Response Code : " + responseCode);

        // Create expected result
        TelemetryItem expectedResult = new TelemetryItem(DocumentType.Requests, requestId);
        expectedResult.setProperty("port", Integer.toString(uri.getPort()));
        expectedResult.setProperty("responseCode", responseCode.toString());
        expectedResult.setProperty("uri", uri.toString());
        expectedResult.setProperty("userAgent", config.getUserAgent());

        try {
            String[] params = uri.getQuery().split("&");
            for (String param : params) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                expectedResult.setProperty("queryParameter." + name, value);
            }
        }
        catch (Exception e) {
            System.out.println("Failed to process query parameters '" + uri.getQuery() + "'. Ignoring.");
        }

        return expectedResult;
    }

    /**
     * Connects to Azure export and retrieves the telemetry items
     * @param docType The document type to retrieve from Azure
     * @return The retrieved telemetry items
     * @throws Exception
     */
    private ArrayList<TelemetryItem> getTelemetryFromAzure(DocumentType docType) throws Exception {
        System.out.println("Creating Azure storage account connection");
        CloudStorageAccount account = CloudStorageAccount.parse(config.getStorageConnectionString());
        CloudBlobClient blobClient = account.createCloudBlobClient();

        System.out.println("Creating Azure queue connection");
        CloudQueueClient queueClient = account.createCloudQueueClient();
        CloudQueue queue = queueClient.getQueueReference(config.getQueueName());

        ArrayList<JSONObject> telemetryAsJson = new ArrayList<JSONObject>();
        StopWatch stopWatch = new StopWatch();

        System.out.println("Starting to poll the queue for " + config.getSecondsToPoll() + "seconds ...");
        stopWatch.start();

        while (stopWatch.getTime() < config.getSecondsToPoll() * millisecondsInSecond) {
            System.out.println(stopWatch.getTime() / millisecondsInSecond + " seconds passed. Got " + telemetryAsJson.size() + " items so far.");

            ArrayList<CloudQueueMessage> messages = (ArrayList<CloudQueueMessage>)
                    queue.retrieveMessages(config.getNumberOfMessagesToRetrieve(), config.getSecondsToPoll(), null, null);
            ArrayList<String> blobUris = getBlobUrisFromQueueMessages(docType, messages);
            for (String blobUri : blobUris) {
                CloudBlockBlob blob = new CloudBlockBlob(new URI(blobUri), blobClient);
                ArrayList<JSONObject> jsonsFromBlobContent = getJsonsFromString(blob.downloadText());
                telemetryAsJson.addAll(jsonsFromBlobContent);
            }

            if (messages.size() < config.getNumberOfMessagesToRetrieve()){
                Helpers.sleep(config.getSecondsToSleep() * millisecondsInSecond);
            }
        }

        ArrayList<TelemetryItem> telemetryItems = new ArrayList<TelemetryItem>();
        for (JSONObject json : telemetryAsJson) {
            telemetryItems.add(getTelemetryItemFromJson(docType, json));
        }

        return telemetryItems;
    }

    /**
     * Gets a list of blob URIs from Azure queue messages
     * @param docType The document type to retrieve from Azure
     * @param messages The Azure queue messages
     * @return An ArrayList of blob URIs
     * @throws Exception
     */
    private ArrayList<String> getBlobUrisFromQueueMessages(DocumentType docType, ArrayList<CloudQueueMessage> messages) throws Exception {
        ArrayList<String> blobUris = new ArrayList<String>();

        System.out.println("Extracting blob URIs of document type " + docType.toString() + " from " + messages.size() + " messages");
        for (CloudQueueMessage message : messages) {
            JSONObject messageContentAsJson = new JSONObject(message.getMessageContentAsString());
            String msgDocType = messageContentAsJson.getString("DocumentType");

            if(msgDocType.equals(docType.toString())) {
                blobUris.add(messageContentAsJson.getString("BlobUri"));
            }
        }

        System.out.println("Got " + blobUris.size() + " blob URIs with document type " + docType.toString());
        return blobUris;
    }

    /**
     * Converts a string to multiple JSON objects
     * @param jString The string to convert
     * @return ArrayList of JSON objects
     */
    private ArrayList<JSONObject> getJsonsFromString(String jString) throws JSONException {
        System.out.println("Extracting JSON objects from string");
        String[] jsonStrings = jString.split("\n");
        ArrayList<JSONObject> jsonObjects = new ArrayList<JSONObject>();

        for(String s : jsonStrings) {
            jsonObjects.add(new JSONObject(s));
        }

        System.out.println("Got " + jsonObjects.size() + " JSON objects");
        return jsonObjects;
    }

    /**
     * Converts JSON object to TelemetryItem by it's document type
     * @param docType The document type of the telemetry
     * @param json The JSON object
     * @return A TelemetryItem
     * @throws Exception
     */
    private TelemetryItem getTelemetryItemFromJson(DocumentType docType, JSONObject json) throws Exception {
        if (docType == DocumentType.Requests) {
            return getRequestTelemetryItemFromJson(json);
        }

        if (docType == DocumentType.PerformanceCounters) {
            return getPerformanceCounterTelemetryItemFromJson(json);
        }

        throw new Exception("Can't find document type: " + docType.toString());
    }

    /**
     * Converts JSON object to Request TelemetryItem
     * @param json The JSON object
     * @return A TelemetryItem
     */
    private TelemetryItem getRequestTelemetryItemFromJson(JSONObject json) throws URISyntaxException, JSONException {
        System.out.println("Converting JSON object to telemetry item with document type " + DocumentType.Requests.toString());
        String address       = json.getJSONArray("request").getJSONObject(0).getString("url");
        Integer port         = json.getJSONArray("request").getJSONObject(0).getJSONObject("urlData").getInt("port");
        Integer responseCode = json.getJSONArray("request").getJSONObject(0).getInt("responseCode");
        String userAgent     = json.getJSONObject("context").getJSONObject("device").getString("userAgent");

        JSONArray parameters = json.getJSONArray("request").getJSONObject(0).getJSONObject("urlData").getJSONArray("queryParameters");
        Hashtable<String, String> queryParameters = new Hashtable<String, String>();
        for (int i = 0; i < parameters.length(); ++i) {
            String name  = parameters.getJSONObject(i).getString("parameter");
            String value = parameters.getJSONObject(i).getString("value");
            queryParameters.put(name, value);
        }

        TelemetryItem telemetryResult = new TelemetryItem(DocumentType.Requests, queryParameters.get("requestId"));
        telemetryResult.setProperty("uri", address);
        telemetryResult.setProperty("port", port.toString());
        telemetryResult.setProperty("responseCode", responseCode.toString());
        telemetryResult.setProperty("userAgent", userAgent);

        for (String key : queryParameters.keySet()) {
            telemetryResult.setProperty("queryParameter." + key, queryParameters.get(key));
        }

        return telemetryResult;
    }

    /**
     * Converts JSON object to PerformanceCounter TelemetryItem
     * @param json The JSON object
     * @return A TelemetryItem
     */
    private TelemetryItem getPerformanceCounterTelemetryItemFromJson(JSONObject json) throws JSONException {
        System.out.println("Converting JSON object to telemetry item with document type " + DocumentType.PerformanceCounters.toString());
        String category = json.getJSONArray("performanceCounter").getJSONObject(0).getString("categoryName");
        String instance = json.getJSONArray("performanceCounter").getJSONObject(0).getString("instanceName");

        TelemetryItem telemetryResult = new TelemetryItem(DocumentType.PerformanceCounters, ""); // TODO when implementing the usage of performance counters test, think of logic for id.
        telemetryResult.setProperty("category", category);
        telemetryResult.setProperty("instance", instance);

        return telemetryResult;
    }

    /**
     * Tests if the expected telemetry exists in the real telemetry from AppInsights
     * @param realTelemetryItems The real telemetry
     * @param expectedTelemetryItems The expected telemetry
     * @return A collection of expected telemetry items that were not found in the real telemetry. If all items exists, returns empty collection.
     */
    private ArrayList<TelemetryItem> getMissingExpectedTelemetry(ArrayList<TelemetryItem> realTelemetryItems,
                                                                 ArrayList<TelemetryItem> expectedTelemetryItems) {

        Hashtable<String, TelemetryItem> realTelemetryHashtable = new Hashtable<String, TelemetryItem>();
        for (TelemetryItem item : realTelemetryItems) {
            realTelemetryHashtable.put(item.getId(), item);
        }

        ArrayList<TelemetryItem> missingExpectedTelemetry = new ArrayList<TelemetryItem>();
        for (TelemetryItem item : expectedTelemetryItems) {
            if (!realTelemetryHashtable.containsKey(item.getId())) {
                System.out.println("Missing expected telemetry item with id " + item.getId() + " and document type " + item.getDocType());
                missingExpectedTelemetry.add(item);
            }

            TelemetryItem matchingRealTelemetry = realTelemetryHashtable.get(item.getId());
            if (!matchingRealTelemetry.equals(item)) {
                System.out.println("Missmatch with real telemetry for expected telemetry item with id " + item.getId() + " and document type " + item.getDocType());
                missingExpectedTelemetry.add(item);
            }
        }

        return missingExpectedTelemetry;
    }
}
