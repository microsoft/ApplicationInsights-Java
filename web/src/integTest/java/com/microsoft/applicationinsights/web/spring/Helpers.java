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
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * Created by moralt on 4/30/2015.
 */
public class Helpers {
    public static void sleep(int milliseconds) {
        try {
            System.out.println("Sleeping for " + milliseconds + " milliseconds Zzz...");
            Thread.sleep(milliseconds);
        } catch(InterruptedException ex) {
            System.out.println("Interrupt caught while sleeping.");
        }
    }

    public static URI constructUrl(String server, int port, String app, String path) throws URISyntaxException {
        return new URI("http://" + server + ":" + port + "/" + app + "/" + path);
    }

    /**
     * Clears an Azure queue from all existing messages
     * @param connectionString Connection string to the storage account
     * @param queueName The name of the queue
     * @throws Exception
     */
    public static void clearAzureQueue(String connectionString, String queueName) throws Exception {
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudQueueClient queueClient = account.createCloudQueueClient();
        CloudQueue queue = queueClient.getQueueReference(queueName);

        queue.downloadAttributes();
        System.out.println("Before clearing: Queue contains " + queue.getApproximateMessageCount() + " items");
        System.out.println("Clearing queue ...");
        queue.clear();
    }

    public static String getRandomUUIDString() {
        return UUID.randomUUID().toString();
    }
}
