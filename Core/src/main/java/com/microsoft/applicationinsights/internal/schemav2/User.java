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
     * Initializes a new instance of the <see cref="User"/> class.
     */
    public User()
    {
        this.InitializeFields();
    }

    /**
     * Gets the AccountAcquisitionDate property.
     */
    public String getAccountAcquisitionDate() {
        return this.accountAcquisitionDate;
    }

    /**
     * Sets the AccountAcquisitionDate property.
     */
    public void setAccountAcquisitionDate(String value) {
        this.accountAcquisitionDate = value;
    }

    /**
     * Gets the AccountId property.
     */
    public String getAccountId() {
        return this.accountId;
    }

    /**
     * Sets the AccountId property.
     */
    public void setAccountId(String value) {
        this.accountId = value;
    }

    /**
     * Gets the UserAgent property.
     */
    public String getUserAgent() {
        return this.userAgent;
    }

    /**
     * Sets the UserAgent property.
     */
    public void setUserAgent(String value) {
        this.userAgent = value;
    }

    /**
     * Gets the Id property.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Sets the Id property.
     */
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
        writer.write("accountAcquisitionDate", accountAcquisitionDate);
        writer.write("accountId", accountId);
        writer.write("userAgent", accountId);
        writer.write("id", id);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }
}
