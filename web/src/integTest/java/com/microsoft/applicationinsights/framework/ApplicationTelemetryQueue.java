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

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;

/**
 * Created by yonisha on 6/16/2015.
 */
public class ApplicationTelemetryQueue {

    private static final int QUEUE_VISIBILITY_TIMEOUT_IN_SECONDS = 180;
    private static final int QUEUE_MESSAGE_BATCH_SIZE = 32;

    private CloudQueue queue;

    public ApplicationTelemetryQueue(String connectionString, String queueName) throws URISyntaxException, StorageException, InvalidKeyException {
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudQueueClient queueClient = account.createCloudQueueClient();
        queue = queueClient.getQueueReference(queueName);

        // This can be a problem if the queue is used by several tests concurrently.
        this.clear();
    }

    public ArrayList<CloudQueueMessage> retrieveMessages() throws StorageException {
        return retrieveMessagesUntilEmpty();
    }

    public void clear() throws StorageException {
        queue.downloadAttributes();
        System.out.println("Clearing " + queue.getApproximateMessageCount() + " items.");

        queue.clear();
    }

    private ArrayList<CloudQueueMessage> retrieveMessagesUntilEmpty() throws StorageException {
        ArrayList<CloudQueueMessage> allMessages = new ArrayList<CloudQueueMessage>();

        do {
            ArrayList<CloudQueueMessage> messages = (ArrayList<CloudQueueMessage>) queue.retrieveMessages(
                    QUEUE_MESSAGE_BATCH_SIZE, QUEUE_VISIBILITY_TIMEOUT_IN_SECONDS, null, null);
            allMessages.addAll(messages);
        } while (allMessages.size() >= QUEUE_MESSAGE_BATCH_SIZE);

        return allMessages;
    }
}
