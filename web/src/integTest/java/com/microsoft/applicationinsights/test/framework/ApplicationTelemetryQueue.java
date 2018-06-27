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

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;

/** Created by yonisha on 6/16/2015. */
public class ApplicationTelemetryQueue {

  private final int queueVisibilityTimeoutInSeconds;
  private final int queueMessageBatchSize;
  private CloudQueue queue;

  public ApplicationTelemetryQueue(
      String connectionString,
      String queueName,
      int queueMessageBatchSize,
      int queueVisibilityTimeoutInSeconds)
      throws URISyntaxException, StorageException, InvalidKeyException {
    CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
    CloudQueueClient queueClient = account.createCloudQueueClient();

    this.queue = queueClient.getQueueReference(queueName);
    this.queueVisibilityTimeoutInSeconds = queueVisibilityTimeoutInSeconds;
    this.queueMessageBatchSize = queueMessageBatchSize;

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

  public void deleteMessages(ArrayList<CloudQueueMessage> messages) throws StorageException {
    for (CloudQueueMessage message : messages) {
      this.queue.deleteMessage(message);
    }
  }

  private ArrayList<CloudQueueMessage> retrieveMessagesUntilEmpty() throws StorageException {
    ArrayList<CloudQueueMessage> allMessages = new ArrayList<CloudQueueMessage>();

    do {
      ArrayList<CloudQueueMessage> messages =
          (ArrayList<CloudQueueMessage>)
              queue.retrieveMessages(
                  this.queueMessageBatchSize, this.queueVisibilityTimeoutInSeconds, null, null);
      allMessages.addAll(messages);
    } while (allMessages.size() >= this.queueMessageBatchSize);

    return allMessages;
  }
}
