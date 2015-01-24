/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class User.
 */
public class User implements JsonSerializable {
    /**
     * Backing field for property AccountAcquisitionDate.
     */
    private String accountAcquisitionDate;

    /**
     * Backing field for property AccountId.
     */
    private String accountId;

    /**
     * Backing field for property UserAgent.
     */
    private String userAgent;

    /**
     * Backing field for property Id.
     */
    private String id;

    /**
     * Initializes a new instance of the class.
     */
    public User()
    {
        this.InitializeFields();
    }

    public String getAccountAcquisitionDate() {
        return this.accountAcquisitionDate;
    }

    public void setAccountAcquisitionDate(String value) {
        this.accountAcquisitionDate = value;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public void setAccountId(String value) {
        this.accountId = value;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public void setUserAgent(String value) {
        this.userAgent = value;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String value) {
        this.id = value;
    }

    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
    public void addToHashMap(HashMap<String, String> map)
    {
        if (!(this.accountAcquisitionDate == null)) {
            map.put("accountAcquisitionDate", this.accountAcquisitionDate);
        }
        if (!(this.accountId == null)) {
            map.put("accountId", this.accountId);
        }
        if (!(this.userAgent == null)) {
            map.put("userAgent", this.userAgent);
        }
        if (!(this.id == null)) {
            map.put("id", this.id);
        }
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be throw during serialization.
     */
    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        this.serializeContent(writer);
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be throw during serialization.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("accountAcquisitionDate", accountAcquisitionDate);
        writer.write("accountId", accountId);
        writer.write("userAgent", accountId);
        writer.write("id", id);
    }

    protected void InitializeFields() {
    }
}
