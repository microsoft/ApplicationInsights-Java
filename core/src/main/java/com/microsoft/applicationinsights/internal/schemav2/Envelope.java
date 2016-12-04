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

package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class Envelope.
 */
public class Envelope implements JsonSerializable
{
    /**
     * Backing field for property Ver.
     */
    private int ver = 1;

    /**
     * Backing field for property Name.
     */
    private String name;

    /**
     * Backing field for property Time.
     */
    private String time;

    /**
     * Backing field for property SampleRate.
     */
    private double sampleRate = 100.0;

    /**
     * Backing field for property Seq.
     */
    private String seq;

    /**
     * Backing field for property IKey.
     */
    private String iKey;

    /**
     * Backing field for property Flags.
     */
    private long flags;

    /**
     * Backing field for property DeviceId.
     */
    private String deviceId;

    /**
     * Backing field for property Os.
     */
    private String os;

    /**
     * Backing field for property OsVer.
     */
    private String osVer;

    /**
     * Backing field for property AppId.
     */
    private String appId;

    /**
     * Backing field for property AppVer.
     */
    private String appVer;

    /**
     * Backing field for property UserId.
     */
    private String userId;

    /**
     * Backing field for property Tags.
     */
    private Map<String, String> tags;

    /**
     * Backing field for property Data.
     */
    private Base data;

    /**
     * Initializes a new instance
     */
    public Envelope() {
        this.InitializeFields();
    }

    public int getVer() {
        return this.ver;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String value) {
        this.time = value;
    }

    public double getSampleRate() {
        return this.sampleRate;
    }

    public void setSampleRate(Double value) {
        if (value == null) {
            return;
        }
        this.sampleRate = value;
    }

    public void setSampleRate(double value) {
        this.sampleRate = value;
    }

    public String getSeq() {
        return this.seq;
    }

    public void setSeq(String value) {
        this.seq = value;
    }

    public String getIKey() {
        return this.iKey;
    }

    public void setIKey(String value) {
        this.iKey = value;
    }

    public long getFlags() {
        return this.flags;
    }

    public void setFlags(long value) {
        this.flags = value;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(String value) {
        this.deviceId = value;
    }

    public String getOs() {
        return this.os;
    }

    public void setOs(String value) {
        this.os = value;
    }

    public String getOsVer() {
        return this.osVer;
    }

    public void setOsVer(String value) {
        this.osVer = value;
    }

    public String getAppId() {
        return this.appId;
    }

    public void setAppId(String value) {
        this.appId = value;
    }

    public String getAppVer() {
        return this.appVer;
    }

    public void setAppVer(String value) {
        this.appVer = value;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String value) {
        this.userId = value;
    }

    public Map<String, String> getTags() {
        if (this.tags == null) {
            this.tags = new HashMap<String, String>();
        }

        return this.tags;
    }

    public void setTags(Map<String, String> value) {
        this.tags = value;
    }

    public Base getData() {
        return this.data;
    }

    public void setData(Base value) {
        this.data = value;
        this.setName(data.getEnvelopName());
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be thrown during serialization.
     */
    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        this.serializeContent(writer);
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("ver", ver);
        writer.write("name", name);
        writer.write("time", time);
        writer.write("sampleRate", sampleRate);
        writer.write("seq", seq);
        writer.write("iKey", iKey);
        writer.write("flags", flags);
        writer.write("deviceId", deviceId);
        writer.write("os", os);
        writer.write("osVer", osVer);
        writer.write("appId", appId);
        writer.write("appVer", appVer);
        writer.write("userId", userId);
        writer.write("tags", tags);
        writer.write("data", data);
    }

    protected void InitializeFields() {
    }
}
