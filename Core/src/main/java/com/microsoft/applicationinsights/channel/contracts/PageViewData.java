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
 * Data contract class PageViewData.
 */
public class PageViewData extends EventData implements
    ITelemetry
{
    /**
     * Envelope Name for this telemetry.
     */
    public static final String EnvelopeName = "Microsoft.ApplicationInsights.PageView";
    
    /**
     * Base Type for this telemetry.
     */
    public static final String BaseType = "Microsoft.ApplicationInsights.PageViewData";
    
    /**
     * Backing field for property Url.
     */
    private String url;
    
    /**
     * Backing field for property Duration.
     */
    private String duration;
    
    /**
     * Initializes a new instance of the <see cref="PageViewData"/> class.
     */
    public PageViewData()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the Url property.
     */
    public String getUrl() {
        return this.url;
    }
    
    /**
     * Sets the Url property.
     */
    public void setUrl(String value) {
        this.url = value;
    }
    
    /**
     * Gets the Duration property.
     */
    public String getDuration() {
        return this.duration;
    }
    
    /**
     * Sets the Duration property.
     */
    public void setDuration(String value) {
        this.duration = value;
    }
    

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected String serializeContent(Writer writer) throws IOException
    {
        String prefix = super.serializeContent(writer);
        if (!(this.url == null))
        {
            writer.write(prefix + "\"url\":");
            writer.write(JsonHelper.convert(this.url));
            prefix = ",";
        }
        
        if (!(this.duration == null))
        {
            writer.write(prefix + "\"duration\":");
            writer.write(JsonHelper.convert(this.duration));
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
