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
 * Data contract class Internal.
 */
public class Internal implements
    IJsonSerializable
{
    /**
     * Backing field for property SdkVersion.
     */
    private String sdkVersion;
    
    /**
     * Backing field for property AgentVersion.
     */
    private String agentVersion;
    
    /**
     * Initializes a new instance of the <see cref="Internal"/> class.
     */
    public Internal()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the SdkVersion property.
     */
    public String getSdkVersion() {
        return this.sdkVersion;
    }
    
    /**
     * Sets the SdkVersion property.
     */
    public void setSdkVersion(String value) {
        this.sdkVersion = value;
    }
    
    /**
     * Gets the AgentVersion property.
     */
    public String getAgentVersion() {
        return this.agentVersion;
    }
    
    /**
     * Sets the AgentVersion property.
     */
    public void setAgentVersion(String value) {
        this.agentVersion = value;
    }
    

    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
    public void addToHashMap(HashMap<String, String> map)
    {
        if (!(this.sdkVersion == null)) {
            map.put("sdkVersion", this.sdkVersion);
        }
        if (!(this.agentVersion == null)) {
            map.put("agentVersion", this.agentVersion);
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
        if (!(this.sdkVersion == null))
        {
            writer.write(prefix + "\"sdkVersion\":");
            writer.write(JsonHelper.convert(this.sdkVersion));
            prefix = ",";
        }
        
        if (!(this.agentVersion == null))
        {
            writer.write(prefix + "\"agentVersion\":");
            writer.write(JsonHelper.convert(this.agentVersion));
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
