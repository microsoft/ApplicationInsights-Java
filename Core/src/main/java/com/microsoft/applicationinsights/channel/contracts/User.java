package com.microsoft.applicationinsights.channel.contracts;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.ArrayList;
import com.microsoft.applicationinsights.channel.contracts.shared.ITelemetry;
import com.microsoft.applicationinsights.channel.contracts.shared.ITelemetryData;
import com.microsoft.applicationinsights.channel.contracts.shared.IContext;
import com.microsoft.applicationinsights.channel.contracts.shared.IJsonSerializable;
import com.microsoft.applicationinsights.channel.contracts.shared.JsonHelper;

/**
 * Data contract class User.
 */
public class User implements
    IJsonSerializable
{
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
    public void serialize(Writer writer) throws IOException
    {
        if (writer == null)
        {
            throw new IllegalArgumentException("writer");
        }
        
        writer.write('{');
        this.serializeContent(writer);
        writer.write('}');
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected String serializeContent(Writer writer) throws IOException
    {
        String prefix = "";
        if (!(this.accountAcquisitionDate == null))
        {
            writer.write(prefix + "\"accountAcquisitionDate\":");
            writer.write(JsonHelper.convert(this.accountAcquisitionDate));
            prefix = ",";
        }
        
        if (!(this.accountId == null))
        {
            writer.write(prefix + "\"accountId\":");
            writer.write(JsonHelper.convert(this.accountId));
            prefix = ",";
        }
        
        if (!(this.userAgent == null))
        {
            writer.write(prefix + "\"userAgent\":");
            writer.write(JsonHelper.convert(this.userAgent));
            prefix = ",";
        }
        
        if (!(this.id == null))
        {
            writer.write(prefix + "\"id\":");
            writer.write(JsonHelper.convert(this.id));
            prefix = ",";
        }
        
        return prefix;
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
