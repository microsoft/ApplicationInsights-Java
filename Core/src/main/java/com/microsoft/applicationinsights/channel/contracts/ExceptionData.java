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
 * Data contract class ExceptionData.
 */
public class ExceptionData extends Domain implements
    ITelemetry
{
    /**
     * Envelope Name for this telemetry.
     */
    public static final String EnvelopeName = "Microsoft.ApplicationInsights.Exception";
    
    /**
     * Base Type for this telemetry.
     */
    public static final String BaseType = "Microsoft.ApplicationInsights.ExceptionData";
    
    /**
     * Backing field for property Ver.
     */
    private int ver = 2;
    
    /**
     * Backing field for property HandledAt.
     */
    private String handledAt;
    
    /**
     * Backing field for property Exceptions.
     */
    private ArrayList<ExceptionDetails> exceptions;
    
    /**
     * Backing field for property SeverityLevel.
     */
    private int severityLevel;
    
    /**
     * Backing field for property Properties.
     */
    private HashMap<String, String> properties;
    
    /**
     * Backing field for property Measurements.
     */
    private HashMap<String, Double> measurements;
    
    /**
     * Initializes a new instance of the <see cref="ExceptionData"/> class.
     */
    public ExceptionData()
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
     * Gets the HandledAt property.
     */
    public String getHandledAt() {
        return this.handledAt;
    }
    
    /**
     * Sets the HandledAt property.
     */
    public void setHandledAt(String value) {
        this.handledAt = value;
    }
    
    /**
     * Gets the Exceptions property.
     */
    public ArrayList<ExceptionDetails> getExceptions() {
        if (this.exceptions == null) {
            this.exceptions = new ArrayList<ExceptionDetails>();
        }
        return this.exceptions;
    }
    
    /**
     * Sets the Exceptions property.
     */
    public void setExceptions(ArrayList<ExceptionDetails> value) {
        this.exceptions = value;
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
     * Gets the Measurements property.
     */
    public HashMap<String, Double> getMeasurements() {
        if (this.measurements == null) {
            this.measurements = new HashMap<String, Double>();
        }
        return this.measurements;
    }
    
    /**
     * Sets the Measurements property.
     */
    public void setMeasurements(HashMap<String, Double> value) {
        this.measurements = value;
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
        
        writer.write(prefix + "\"handledAt\":");
        writer.write(JsonHelper.convert(this.handledAt));
        prefix = ",";
        
        writer.write(prefix + "\"exceptions\":");
        JsonHelper.writeList(writer, this.exceptions);
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
        
        if (!(this.measurements == null))
        {
            writer.write(prefix + "\"measurements\":");
            JsonHelper.writeDictionary(writer, this.measurements);
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
