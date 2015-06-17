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

package com.microsoft.applicationinsights.framework;

import com.microsoft.applicationinsights.framework.telemetries.DocumentType;
import com.microsoft.applicationinsights.framework.telemetries.TelemetryItem;
import com.microsoft.applicationinsights.framework.telemetries.TelemetryItemFactory;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import org.apache.commons.lang3.time.StopWatch;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by yonisha on 6/16/2015.
 */
public class ApplicationTelemetryManager {

    private static final int POLLING_TIMEOUT_IN_SECONDS = 180;
    private static final int QUEUE_POLLING_INTERVAL_IN_SECONDS = 3;
    private static final int MS_IN_SECOND = 1000;

    private ApplicationTelemetryQueue applicationTelemetryQueue;
    private ApplicationBlobClient applicationBlobClient;

    public ApplicationTelemetryManager(String storageAccountConnectionString, String queueName) throws StorageException, InvalidKeyException, URISyntaxException {
        this.applicationTelemetryQueue = new ApplicationTelemetryQueue(storageAccountConnectionString, queueName);
        this.applicationBlobClient = new ApplicationBlobClient(storageAccountConnectionString);
    }

    public HashSet<TelemetryItem> getApplicationTelemetries(DocumentType docType, int numberOfExpectedTelemetries) throws Exception {
        ArrayList<JSONObject> telemetryAsJson = getApplicationTelemetriesAsJson(docType, numberOfExpectedTelemetries);

        if (telemetryAsJson.size() < numberOfExpectedTelemetries) {
            String message = String.format("Got only %d out of %d expected telemetries within the timeout defined by (%d seconds)",
                    telemetryAsJson.size(),
                    numberOfExpectedTelemetries,
                    POLLING_TIMEOUT_IN_SECONDS);

            throw new TimeoutException(message);
        }

        HashSet<TelemetryItem> telemetryItems = new HashSet<TelemetryItem>();
        for (JSONObject json : telemetryAsJson) {
            TelemetryItem telemetryItem = TelemetryItemFactory.createTelemetryItem(docType, json);
            telemetryItems.add(telemetryItem);
        }

        return telemetryItems;
    }

    private ArrayList<JSONObject> getApplicationTelemetriesAsJson(DocumentType docType, int numberOfExpectedTelemetries) throws Exception {
        ArrayList<JSONObject> telemetryAsJson = new ArrayList<JSONObject>();
        long maxWaitTimeInMillis = POLLING_TIMEOUT_IN_SECONDS * MS_IN_SECOND;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        while (telemetryAsJson.size() < numberOfExpectedTelemetries && stopWatch.getTime() < maxWaitTimeInMillis) {
            ArrayList<CloudQueueMessage> messages = applicationTelemetryQueue.retrieveMessages();

            ArrayList<String> blobUris = getBlobUrisFromQueueMessages(docType, messages);
            for (String blobUri : blobUris) {
                List<JSONObject> telemetriesAsJsonObjects = applicationBlobClient.getTelemetriesAsJsonObjects(blobUri);

                telemetryAsJson.addAll(telemetriesAsJsonObjects);
            }

            sleepSafe();
        }

        return telemetryAsJson;
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

    private void sleepSafe() {
        int timeToSleepInMilliseconds = QUEUE_POLLING_INTERVAL_IN_SECONDS * MS_IN_SECOND;

        try {
            System.out.println("Sleeping for " + QUEUE_POLLING_INTERVAL_IN_SECONDS + " seconds...");
            Thread.sleep(timeToSleepInMilliseconds);
        } catch(InterruptedException ex) {
        }
    }
}
