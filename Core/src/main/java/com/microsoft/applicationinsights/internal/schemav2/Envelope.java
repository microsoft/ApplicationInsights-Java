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
     * Initializes a new instance of the <see cref="Envelope"/> class.
     */
    public Envelope() {
        this.InitializeFields();
    }

    /**
     * Gets the Ver property.
     */
    public int getVer() {
        return this.ver;
    }

    /**
     * Gets the Name property.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the Name property.
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the Time property.
     */
    public String getTime() {
        return this.time;
    }

    /**
     * Sets the Time property.
     */
    public void setTime(String value) {
        this.time = value;
    }

    /**
     * Gets the SampleRate property.
     */
    public double getSampleRate() {
        return this.sampleRate;
    }

    /**
     * Sets the SampleRate property.
     */
    public void setSampleRate(double value) {
        this.sampleRate = value;
    }

    /**
     * Gets the Seq property.
     */
    public String getSeq() {
        return this.seq;
    }

    /**
     * Sets the Seq property.
     */
    public void setSeq(String value) {
        this.seq = value;
    }

    /**
     * Gets the IKey property.
     */
    public String getIKey() {
        return this.iKey;
    }

    /**
     * Sets the IKey property.
     */
    public void setIKey(String value) {
        this.iKey = value;
    }

    /**
     * Gets the Flags property.
     */
    public long getFlags() {
        return this.flags;
    }

    /**
     * Sets the Flags property.
     */
    public void setFlags(long value) {
        this.flags = value;
    }

    /**
     * Gets the DeviceId property.
     */
    public String getDeviceId() {
        return this.deviceId;
    }

    /**
     * Sets the DeviceId property.
     */
    public void setDeviceId(String value) {
        this.deviceId = value;
    }

    /**
     * Gets the Os property.
     */
    public String getOs() {
        return this.os;
    }

    /**
     * Sets the Os property.
     */
    public void setOs(String value) {
        this.os = value;
    }

    /**
     * Gets the OsVer property.
     */
    public String getOsVer() {
        return this.osVer;
    }

    /**
     * Sets the OsVer property.
     */
    public void setOsVer(String value) {
        this.osVer = value;
    }

    /**
     * Gets the AppId property.
     */
    public String getAppId() {
        return this.appId;
    }

    /**
     * Sets the AppId property.
     */
    public void setAppId(String value) {
        this.appId = value;
    }

    /**
     * Gets the AppVer property.
     */
    public String getAppVer() {
        return this.appVer;
    }

    /**
     * Sets the AppVer property.
     */
    public void setAppVer(String value) {
        this.appVer = value;
    }

    /**
     * Gets the UserId property.
     */
    public String getUserId() {
        return this.userId;
    }

    /**
     * Sets the UserId property.
     */
    public void setUserId(String value) {
        this.userId = value;
    }

    /**
     * Gets the Tags property.
     */
    public Map<String, String> getTags() {
        if (this.tags == null) {
            this.tags = new HashMap<String, String>();
        }

        return this.tags;
    }

    /**
     * Sets the Tags property.
     */
    public void setTags(Map<String, String> value) {
        this.tags = value;
    }

    /**
     * Gets the Data property.
     */
    public Base getData() {
        return this.data;
    }

    /**
     * Sets the Data property.
     */
    public void setData(Base value) {
        this.data = value;
        this.setName(data.getEnvelopName());
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        this.serializeContent(writer);
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
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
