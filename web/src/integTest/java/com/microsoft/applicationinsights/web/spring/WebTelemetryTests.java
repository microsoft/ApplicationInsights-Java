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

package com.microsoft.applicationinsights.web.spring;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import org.apache.commons.lang3.time.StopWatch;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by moralt on 05/05/2015.
 */
public class WebTelemetryTests {
    String runId = Helpers.getRandomUUIDString();
    TestEnvironment testEnv;
    TestSettings testSettings;
    private static final int millisecondsInSecond = 1000;

    /**
     * Sends GET requests to server and expects that will telemetry from app insights and it will include the correct information about the request
     * @throws Exception
     */
    @Test
    public void sendHttpRequestsTest() throws Exception {

        initTestConfiguration();

        HashSet<TelemetryItem> expectedTelemetries = sendHttpGetRequests();
        HashSet<TelemetryItem> realTelemetries = getLoggedTelemetry(DocumentType.Requests);

        HashSet<TelemetryItem> missingTelemetry = getMismatchingTelemetryItems(realTelemetries, expectedTelemetries);
        if (missingTelemetry.size() > 0) {
            String errorRequests = "";
            for (TelemetryItem item : missingTelemetry) {
                errorRequests += "\n" + item.getProperty("uri");
                System.out.println("Didn't find matching item in real telemetry for request of URI " + item.getProperty("uri"));
            }

            throw new Exception("Didn't find match for " + missingTelemetry.size() + " items.\nError HTTP requests:" + errorRequests);
        }
        else {
            System.out.println("Test passed successfully");
        }
    }

    private void initTestConfiguration() throws Exception {
        testEnv = new TestEnvironment();
        testSettings = new TestSettings();
        Helpers.clearAzureQueue(testEnv.getApplicationStorageConnectionString(), testEnv.getApplicationStorageExportQueueName());
    }

    private HashSet<TelemetryItem> sendHttpGetRequests() throws Exception {

        String serverAddress = testEnv.getApplicationServer();
        int port = testEnv.getApplicationServerPort();
        String applicationName = testEnv.getApplicationName();

        ArrayList<String> uriPathsToRequest = new ArrayList<String>();
        uriPathsToRequest.add("books?id=Thriller&runId=" + runId);
        uriPathsToRequest.add("loan?title=Gone%20Girl&id=030758836x&subject=Thriller&runId=" + runId);
        uriPathsToRequest.add("nonExistingWebFage?runId=" + runId);

        HashSet<TelemetryItem> expectedTelemetries = new HashSet<TelemetryItem>();

        for (String uriPath : uriPathsToRequest) {
            String requestId = Helpers.getRandomUUIDString();
            URI uri = Helpers.constructUrl(serverAddress, port, applicationName, uriPath + "&requestId=" + requestId);
            int responseCode = sendHttpGetRequest(uri);
            TelemetryItem expectedTelemetry = createExpectedResult(uri, requestId, responseCode);
            expectedTelemetries.add(expectedTelemetry);
        }

        return expectedTelemetries;
    }

    /**
     * Sends HTTP GET request and returns the expected telemetry from AppInsights
     * @param uri The URI for the request
     * @return The expected telemetry items from AppInsights
     * @throws Exception
     */
    private int sendHttpGetRequest(URI uri) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

        // optional default is GET
        connection.setRequestMethod("GET");

        System.out.println("Sending 'GET' request to URL: " + uri.toString());
        Integer responseCode = connection.getResponseCode();
        System.out.println("Response Code : " + responseCode);

        return responseCode;
    }

    /**
     * Creates expected HTTP request result
     * @param uri The URI for the request
     * @param requestId UUID of the request
     * @param responseCode The expected response code for HTTP request with this URI
     * @return A TelemetryItem with the expected results
     */
    private TelemetryItem createExpectedResult(URI uri, String requestId, Integer responseCode) {
        // Create expected result
        TelemetryItem telemetryItem = new RequestTelemetryItem();
        telemetryItem.setProperty("id", requestId);
        telemetryItem.setProperty("port", Integer.toString(uri.getPort()));
        telemetryItem.setProperty("responseCode", responseCode.toString());
        telemetryItem.setProperty("uri", uri.toString());

        String[] params = uri.getQuery().split("&");
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            telemetryItem.setProperty("queryParameter." + name, value);
        }

        return telemetryItem;
    }

    /**
     * Connects to Azure export and retrieves the telemetry items
     * @param docType The document type to retrieve from Azure
     * @return The retrieved telemetry items
     * @throws Exception
     */
    private HashSet<TelemetryItem> getLoggedTelemetry(DocumentType docType) throws Exception {
        System.out.println("Creating Azure storage account connection");
        CloudStorageAccount account = CloudStorageAccount.parse(testEnv.getApplicationStorageConnectionString());
        CloudBlobClient blobClient = account.createCloudBlobClient();

        System.out.println("Creating Azure queue connection");
        CloudQueueClient queueClient = account.createCloudQueueClient();
        CloudQueue queue = queueClient.getQueueReference(testEnv.getApplicationStorageExportQueueName());

        ArrayList<JSONObject> telemetryAsJson = new ArrayList<JSONObject>();
        StopWatch stopWatch = new StopWatch();

        System.out.println("Starting to poll the queue for " + testSettings.getPollingInterval() + "seconds ...");
        stopWatch.start();

        while (stopWatch.getTime() < testSettings.getMaxWaitTime() * millisecondsInSecond) {
            System.out.println(stopWatch.getTime() / millisecondsInSecond + " seconds passed. Got " + telemetryAsJson.size() + " items so far.");

            ArrayList<CloudQueueMessage> messages = (ArrayList<CloudQueueMessage>)
                    queue.retrieveMessages(testSettings.getMessageBatchSize(), testSettings.getMaxWaitTime(), null, null);
            ArrayList<String> blobUris = getBlobUrisFromQueueMessages(docType, messages);
            for (String blobUri : blobUris) {
                CloudBlockBlob blob = new CloudBlockBlob(new URI(blobUri), blobClient);
                ArrayList<JSONObject> jsonsFromBlobContent = convertToJson(blob.downloadText());
                telemetryAsJson.addAll(jsonsFromBlobContent);
            }

            if (messages.size() < testSettings.getMessageBatchSize()){
                Helpers.sleep(testSettings.getPollingInterval() * millisecondsInSecond);
            }
        }

        HashSet<TelemetryItem> telemetryItems = new HashSet<TelemetryItem>();
        for (JSONObject json : telemetryAsJson) {
            TelemetryItem telemetryItem = TelemetryItemFactory.createTelemetryItem(docType, json);
            telemetryItems.add(telemetryItem);
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

            if (msgDocType.equals(docType.toString())) {
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
    private ArrayList<JSONObject> convertToJson(String jString) throws JSONException {
        System.out.println("Extracting JSON objects from string");
        String[] jsonStrings = jString.split("\n");
        ArrayList<JSONObject> jsonObjects = new ArrayList<JSONObject>();

        for (String s : jsonStrings) {
            jsonObjects.add(new JSONObject(s));
        }

        System.out.println("Got " + jsonObjects.size() + " JSON objects");
        return jsonObjects;
    }

    /**
     * Tests if the expected telemetry exists in the real telemetry from AppInsights
     * @param containsTelemetryItems The telemetry items that should contain all telemetry items in 'containedTelemetryItems'
     * @param containedTelemetryItems The telemetry items that should be contained in the 'containsTelemetryItems'
     * @return A collection of telemetry items from 'containedTelemetryItems' that are not in 'containsTelemetryItems'
     */
    private HashSet<TelemetryItem> getMismatchingTelemetryItems(HashSet<TelemetryItem> containsTelemetryItems,
                                                                HashSet<TelemetryItem> containedTelemetryItems) {

        HashSet<TelemetryItem> missingExpectedTelemetry = new HashSet<TelemetryItem>();

        for (TelemetryItem item : containedTelemetryItems) {
            if (!containsTelemetryItems.contains(item)) {
                System.out.println("Missing expected telemetry item with document type " + item.getDocType());
                missingExpectedTelemetry.add(item);
            }
        }

        return missingExpectedTelemetry;
    }
}
