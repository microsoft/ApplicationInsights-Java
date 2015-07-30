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

package com.microsoft.applicationinsights.test.framework;

import com.microsoft.applicationinsights.test.framework.telemetries.DocumentType;
import com.microsoft.applicationinsights.test.framework.telemetries.TelemetryItem;
import com.microsoft.applicationinsights.test.framework.telemetries.TelemetryItemFactory;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import org.apache.commons.lang3.time.StopWatch;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by yonisha on 6/16/2015.
 */
public class ApplicationTelemetryManager {

    private static final String RUN_ID_QUERY_PARAM_NAME = "runId";
    private static final int MS_IN_SECOND = 1000;

    private final int pollingTimeoutInSeconds;
    private final int queuePollingIntervalInSeconds;
    private List<TelemetryItem> cachedTelemetries = new ArrayList<TelemetryItem>();
    private ApplicationTelemetryQueue applicationTelemetryQueue;
    private ApplicationBlobClient applicationBlobClient;

    public ApplicationTelemetryManager(
            String storageAccountConnectionString,
            String queueName,
            int pollingTimeoutInSeconds,
            int queuePollingIntervalInSeconds,
            int queueMessageBatchSize) throws StorageException, InvalidKeyException, URISyntaxException {
        this.applicationTelemetryQueue = new ApplicationTelemetryQueue(storageAccountConnectionString, queueName, queueMessageBatchSize, pollingTimeoutInSeconds);
        this.applicationBlobClient = new ApplicationBlobClient(storageAccountConnectionString);
        this.pollingTimeoutInSeconds = pollingTimeoutInSeconds;
        this.queuePollingIntervalInSeconds = queuePollingIntervalInSeconds;
    }

    public List<TelemetryItem> getApplicationTelemetries(String runId, int numberOfExpectedTelemetries) throws Exception {

        List<TelemetryItem> telemetriesForRunId = getTelemetriesFromCacheWithRunId(runId);

        if (telemetriesForRunId.size() < numberOfExpectedTelemetries) {
            int missingTelemetriesCount = numberOfExpectedTelemetries - telemetriesForRunId.size();
            updateTelemetryCache(runId, missingTelemetriesCount);
        }

        telemetriesForRunId = getTelemetriesFromCacheWithRunId(runId);

        if (telemetriesForRunId.size() < numberOfExpectedTelemetries) {
            String message = String.format("Got only %d out of %d expected telemetries within the timeout (%d seconds)",
                    telemetriesForRunId.size(),
                    numberOfExpectedTelemetries,
                    this.pollingTimeoutInSeconds);

            throw new TimeoutException(message);
        }

        return telemetriesForRunId;
    }

    private List<TelemetryItem> getTelemetriesFromCacheWithRunId(String runId) {
        List<TelemetryItem> telemetryForRunId = new ArrayList<TelemetryItem>();

        for (TelemetryItem telemetryItem : this.cachedTelemetries) {
            String runIdProperty = telemetryItem.getProperty(RUN_ID_QUERY_PARAM_NAME);
            if (runIdProperty != null && runIdProperty.equalsIgnoreCase(runId)) {
                telemetryForRunId.add(telemetryItem);
            }
        }

        return telemetryForRunId;
    }

    private void updateTelemetryCache(String runId, int numberOfExpectedTelemetries) throws Exception {
        long maxWaitTimeInMillis = this.pollingTimeoutInSeconds * MS_IN_SECOND;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int countOfTelemetriesWithExpectedRunId = 0;
        while (countOfTelemetriesWithExpectedRunId < numberOfExpectedTelemetries && stopWatch.getTime() < maxWaitTimeInMillis) {
            ArrayList<CloudQueueMessage> messages = applicationTelemetryQueue.retrieveMessages();

            ArrayList<TelemetryBlob> telemetryBlobs = getBlobUrisFromQueueMessages(messages);
            for (TelemetryBlob telemetryBlob : telemetryBlobs) {
                List<JSONObject> telemetriesAsJsonObjects = applicationBlobClient.getTelemetriesAsJsonObjects(telemetryBlob.getBlobUri());

                for (JSONObject json : telemetriesAsJsonObjects) {
                    TelemetryItem telemetryItem = TelemetryItemFactory.createTelemetryItem(telemetryBlob.getDocType(), json);
                    this.cachedTelemetries.add(telemetryItem);

                    String runIdProperty = telemetryItem.getProperty(RUN_ID_QUERY_PARAM_NAME);
                    if (!LocalStringsUtils.isNullOrEmpty(runIdProperty) && runIdProperty.equalsIgnoreCase(runId)) {
                        countOfTelemetriesWithExpectedRunId++;
                    }
                }
            }

            applicationTelemetryQueue.deleteMessages(messages);

            if (countOfTelemetriesWithExpectedRunId < numberOfExpectedTelemetries) {
                sleepSafe();
            }
        }
    }

    /**
     * Gets a list of blob URIs from Azure queue messages
     * @param messages The Azure queue messages
     * @return An ArrayList of blob URIs
     * @throws Exception
     */
    private ArrayList<TelemetryBlob> getBlobUrisFromQueueMessages(ArrayList<CloudQueueMessage> messages) throws Exception {
        ArrayList<TelemetryBlob> telemetryBlobs = new ArrayList<TelemetryBlob>();

        for (CloudQueueMessage message : messages) {
            JSONObject messageContentAsJson = new JSONObject(message.getMessageContentAsString());
            String msgDocType = messageContentAsJson.getString("DocumentType");
            telemetryBlobs.add(new TelemetryBlob(DocumentType.valueOf(msgDocType), messageContentAsJson.getString("BlobUri")));
        }

        return telemetryBlobs;
    }

    private void sleepSafe() {
        int timeToSleepInMilliseconds = this.queuePollingIntervalInSeconds * MS_IN_SECOND;

        try {
            System.out.println("Sleeping for " + this.queuePollingIntervalInSeconds + " seconds...");
            Thread.sleep(timeToSleepInMilliseconds);
        } catch(InterruptedException ex) {
        }
    }
}
