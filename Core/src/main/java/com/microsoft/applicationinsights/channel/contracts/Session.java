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
 * Data contract class Session.
 */
public class Session implements
    IJsonSerializable
{
    /**
     * Backing field for property Id.
     */
    private String id;
    
    /**
     * Backing field for property IsFirst.
     */
    private String isFirst;
    
    /**
     * Backing field for property IsNew.
     */
    private String isNew;
    
    /**
     * Initializes a new instance of the <see cref="Session"/> class.
     */
    public Session()
    {
        this.InitializeFields();
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
     * Gets the IsFirst property.
     */
    public String getIsFirst() {
        return this.isFirst;
    }
    
    /**
     * Sets the IsFirst property.
     */
    public void setIsFirst(String value) {
        this.isFirst = value;
    }
    
    /**
     * Gets the IsNew property.
     */
    public String getIsNew() {
        return this.isNew;
    }
    
    /**
     * Sets the IsNew property.
     */
    public void setIsNew(String value) {
        this.isNew = value;
    }
    

    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
    public void addToHashMap(HashMap<String, String> map)
    {
        if (!(this.id == null)) {
            map.put("id", this.id);
        }
        if (!(this.isFirst == null)) {
            map.put("isFirst", this.isFirst);
        }
        if (!(this.isNew == null)) {
            map.put("isNew", this.isNew);
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
        if (!(this.id == null))
        {
            writer.write(prefix + "\"id\":");
            writer.write(JsonHelper.convert(this.id));
            prefix = ",";
        }
        
        if (!(this.isFirst == null))
        {
            writer.write(prefix + "\"isFirst\":");
            writer.write(JsonHelper.convert(this.isFirst));
            prefix = ",";
        }
        
        if (!(this.isNew == null))
        {
            writer.write(prefix + "\"isNew\":");
            writer.write(JsonHelper.convert(this.isNew));
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
