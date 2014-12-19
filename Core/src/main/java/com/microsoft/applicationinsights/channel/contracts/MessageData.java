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
 * Data contract class MessageData.
 */
public class MessageData extends Domain implements
    ITelemetry
{
    /**
     * Envelope Name for this telemetry.
     */
    public static final String EnvelopeName = "Microsoft.ApplicationInsights.Message";
    
    /**
     * Base Type for this telemetry.
     */
    public static final String BaseType = "Microsoft.ApplicationInsights.MessageData";
    
    /**
     * Backing field for property Ver.
     */
    private int ver = 2;
    
    /**
     * Backing field for property Message.
     */
    private String message;
    
    /**
     * Backing field for property SeverityLevel.
     */
    private int severityLevel;
    
    /**
     * Backing field for property Properties.
     */
    private HashMap<String, String> properties;
    
    /**
     * Initializes a new instance of the <see cref="MessageData"/> class.
     */
    public MessageData()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the Ver property.
     */
    public int getVer() {
        return this.ver;
    }
    
    /**
     * Sets the Ver property.
     */
    public void setVer(int value) {
        this.ver = value;
    }
    
    /**
     * Gets the Message property.
     */
    public String getMessage() {
        return this.message;
    }
    
    /**
     * Sets the Message property.
     */
    public void setMessage(String value) {
        this.message = value;
    }
    
    /**
     * Gets the SeverityLevel property.
     */
    public int getSeverityLevel() {
        return this.severityLevel;
    }
    
    /**
     * Sets the SeverityLevel property.
     */
    public void setSeverityLevel(int value) {
        this.severityLevel = value;
    }
    
    /**
     * Gets the Properties property.
     */
    public HashMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new HashMap<String, String>();
        }
        return this.properties;
    }
    
    /**
     * Sets the Properties property.
     */
    public void setProperties(HashMap<String, String> value) {
        this.properties = value;
    }
    

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected String serializeContent(Writer writer) throws IOException
    {
        String prefix = super.serializeContent(writer);
        writer.write(prefix + "\"ver\":");
        writer.write(JsonHelper.convert(this.ver));
        prefix = ",";
        
        writer.write(prefix + "\"message\":");
        writer.write(JsonHelper.convert(this.message));
        prefix = ",";
        
        if (!(this.severityLevel == 0))
        {
            writer.write(prefix + "\"severityLevel\":");
            writer.write(JsonHelper.convert(this.severityLevel));
            prefix = ",";
        }
        
        if (!(this.properties == null))
        {
            writer.write(prefix + "\"properties\":");
            JsonHelper.writeDictionary(writer, this.properties);
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
