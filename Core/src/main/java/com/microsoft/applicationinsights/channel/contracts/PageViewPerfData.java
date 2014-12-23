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
 * Data contract class PageViewPerfData.
 */
public class PageViewPerfData extends PageViewData implements
    ITelemetry
{
    /**
     * Envelope Name for this telemetry.
     */
    public static final String EnvelopeName = "Microsoft.ApplicationInsights.PageViewPerf";
    
    /**
     * Base Type for this telemetry.
     */
    public static final String BaseType = "Microsoft.ApplicationInsights.PageViewPerfData";
    
    /**
     * Backing field for property PerfTotal.
     */
    private String perfTotal;
    
    /**
     * Backing field for property NetworkConnect.
     */
    private String networkConnect;
    
    /**
     * Backing field for property SentRequest.
     */
    private String sentRequest;
    
    /**
     * Backing field for property ReceivedResponse.
     */
    private String receivedResponse;
    
    /**
     * Backing field for property DomProcessing.
     */
    private String domProcessing;
    
    /**
     * Initializes a new instance of the <see cref="PageViewPerfData"/> class.
     */
    public PageViewPerfData()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the PerfTotal property.
     */
    public String getPerfTotal() {
        return this.perfTotal;
    }
    
    /**
     * Sets the PerfTotal property.
     */
    public void setPerfTotal(String value) {
        this.perfTotal = value;
    }
    
    /**
     * Gets the NetworkConnect property.
     */
    public String getNetworkConnect() {
        return this.networkConnect;
    }
    
    /**
     * Sets the NetworkConnect property.
     */
    public void setNetworkConnect(String value) {
        this.networkConnect = value;
    }
    
    /**
     * Gets the SentRequest property.
     */
    public String getSentRequest() {
        return this.sentRequest;
    }
    
    /**
     * Sets the SentRequest property.
     */
    public void setSentRequest(String value) {
        this.sentRequest = value;
    }
    
    /**
     * Gets the ReceivedResponse property.
     */
    public String getReceivedResponse() {
        return this.receivedResponse;
    }
    
    /**
     * Sets the ReceivedResponse property.
     */
    public void setReceivedResponse(String value) {
        this.receivedResponse = value;
    }
    
    /**
     * Gets the DomProcessing property.
     */
    public String getDomProcessing() {
        return this.domProcessing;
    }
    
    /**
     * Sets the DomProcessing property.
     */
    public void setDomProcessing(String value) {
        this.domProcessing = value;
    }
    

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected String serializeContent(Writer writer) throws IOException
    {
        String prefix = super.serializeContent(writer);
        if (!(this.perfTotal == null))
        {
            writer.write(prefix + "\"perfTotal\":");
            writer.write(JsonHelper.convert(this.perfTotal));
            prefix = ",";
        }
        
        if (!(this.networkConnect == null))
        {
            writer.write(prefix + "\"networkConnect\":");
            writer.write(JsonHelper.convert(this.networkConnect));
            prefix = ",";
        }
        
        if (!(this.sentRequest == null))
        {
            writer.write(prefix + "\"sentRequest\":");
            writer.write(JsonHelper.convert(this.sentRequest));
            prefix = ",";
        }
        
        if (!(this.receivedResponse == null))
        {
            writer.write(prefix + "\"receivedResponse\":");
            writer.write(JsonHelper.convert(this.receivedResponse));
            prefix = ",";
        }
        
        if (!(this.domProcessing == null))
        {
            writer.write(prefix + "\"domProcessing\":");
            writer.write(JsonHelper.convert(this.domProcessing));
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
