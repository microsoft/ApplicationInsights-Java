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
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yonisha on 6/16/2015.
 */
public class ApplicationBlobClient {

    private CloudBlobClient cloudBlobClient;

    public ApplicationBlobClient(String connectionString) throws URISyntaxException, InvalidKeyException {
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        cloudBlobClient = account.createCloudBlobClient();
    }

    public List<JSONObject> getTelemetriesAsJsonObjects(String blobUri) throws URISyntaxException, StorageException, JSONException, IOException {

        CloudBlockBlob blob = new CloudBlockBlob(new URI(blobUri), this.cloudBlobClient);
        String blobContent = blob.downloadText();

        ArrayList<JSONObject> jsonObjects = convertToJsonObject(blobContent);

        return jsonObjects;
    }

    /**
     * Converts a string to multiple JSON objects
     * @param jString The string to convert
     * @return ArrayList of JSON objects
     */
    private ArrayList<JSONObject> convertToJsonObject(String jString) throws JSONException {
        System.out.println("Extracting JSON objects from string");
        String[] jsonStrings = jString.split("\n");
        ArrayList<JSONObject> jsonObjects = new ArrayList<JSONObject>();

        for (String s : jsonStrings) {
            jsonObjects.add(new JSONObject(s));
        }

        System.out.println("Got " + jsonObjects.size() + " JSON objects");
        return jsonObjects;
    }
}
