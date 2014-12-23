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
 * Data contract class RemoteDependencyData.
 */
public class RemoteDependencyData extends Domain implements
    ITelemetry
{
    /**
     * Envelope Name for this telemetry.
     */
    public static final String EnvelopeName = "Microsoft.ApplicationInsights.RemoteDependency";
    
    /**
     * Base Type for this telemetry.
     */
    public static final String BaseType = "Microsoft.ApplicationInsights.RemoteDependencyData";
    
    /**
     * Backing field for property Ver.
     */
    private int ver = 2;
    
    /**
     * Backing field for property Name.
     */
    private String name;
    
    /**
     * Backing field for property Kind.
     */
    private int kind = DataPointType.Measurement;
    
    /**
     * Backing field for property Value.
     */
    private double value;
    
    /**
     * Backing field for property Count.
     */
    private Integer count;
    
    /**
     * Backing field for property Min.
     */
    private Double min;
    
    /**
     * Backing field for property Max.
     */
    private Double max;
    
    /**
     * Backing field for property StdDev.
     */
    private Double stdDev;
    
    /**
     * Backing field for property DependencyKind.
     */
    private int dependencyKind = DependencyKind.Undefined;
    
    /**
     * Backing field for property Success.
     */
    private Boolean success = true;
    
    /**
     * Backing field for property Async.
     */
    private Boolean async;
    
    /**
     * Backing field for property DependencySource.
     */
    private int dependencySource = DependencySourceType.Undefined;
    
    /**
     * Backing field for property Properties.
     */
    private HashMap<String, String> properties;
    
    /**
     * Initializes a new instance of the <see cref="RemoteDependencyData"/> class.
     */
    public RemoteDependencyData()
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
     * Gets the Kind property.
     */
    public int getKind() {
        return this.kind;
    }
    
    /**
     * Sets the Kind property.
     */
    public void setKind(int value) {
        this.kind = value;
    }
    
    /**
     * Gets the Value property.
     */
    public double getValue() {
        return this.value;
    }
    
    /**
     * Sets the Value property.
     */
    public void setValue(double value) {
        this.value = value;
    }
    
    /**
     * Gets the Count property.
     */
    public Integer getCount() {
        return this.count;
    }
    
    /**
     * Sets the Count property.
     */
    public void setCount(Integer value) {
        this.count = value;
    }
    
    /**
     * Gets the Min property.
     */
    public Double getMin() {
        return this.min;
    }
    
    /**
     * Sets the Min property.
     */
    public void setMin(Double value) {
        this.min = value;
    }
    
    /**
     * Gets the Max property.
     */
    public Double getMax() {
        return this.max;
    }
    
    /**
     * Sets the Max property.
     */
    public void setMax(Double value) {
        this.max = value;
    }
    
    /**
     * Gets the StdDev property.
     */
    public Double getStdDev() {
        return this.stdDev;
    }
    
    /**
     * Sets the StdDev property.
     */
    public void setStdDev(Double value) {
        this.stdDev = value;
    }
    
    /**
     * Gets the DependencyKind property.
     */
    public int getDependencyKind() {
        return this.dependencyKind;
    }
    
    /**
     * Sets the DependencyKind property.
     */
    public void setDependencyKind(int value) {
        this.dependencyKind = value;
    }
    
    /**
     * Gets the Success property.
     */
    public Boolean getSuccess() {
        return this.success;
    }
    
    /**
     * Sets the Success property.
     */
    public void setSuccess(Boolean value) {
        this.success = value;
    }
    
    /**
     * Gets the Async property.
     */
    public Boolean getAsync() {
        return this.async;
    }
    
    /**
     * Sets the Async property.
     */
    public void setAsync(Boolean value) {
        this.async = value;
    }
    
    /**
     * Gets the DependencySource property.
     */
    public int getDependencySource() {
        return this.dependencySource;
    }
    
    /**
     * Sets the DependencySource property.
     */
    public void setDependencySource(int value) {
        this.dependencySource = value;
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
        
        writer.write(prefix + "\"name\":");
        writer.write(JsonHelper.convert(this.name));
        prefix = ",";
        
        if (!(this.kind == 0))
        {
            writer.write(prefix + "\"kind\":");
            writer.write(JsonHelper.convert(this.kind));
            prefix = ",";
        }
        
        writer.write(prefix + "\"value\":");
        writer.write(JsonHelper.convert(this.value));
        prefix = ",";
        
        if (!(this.count == null))
        {
            writer.write(prefix + "\"count\":");
            writer.write(JsonHelper.convert(this.count));
            prefix = ",";
        }
        
        if (!(this.min == null))
        {
            writer.write(prefix + "\"min\":");
            writer.write(JsonHelper.convert(this.min));
            prefix = ",";
        }
        
        if (!(this.max == null))
        {
            writer.write(prefix + "\"max\":");
            writer.write(JsonHelper.convert(this.max));
            prefix = ",";
        }
        
        if (!(this.stdDev == null))
        {
            writer.write(prefix + "\"stdDev\":");
            writer.write(JsonHelper.convert(this.stdDev));
            prefix = ",";
        }
        
        writer.write(prefix + "\"dependencyKind\":");
        writer.write(JsonHelper.convert(this.dependencyKind));
        prefix = ",";
        
        if (!(this.success == null))
        {
            writer.write(prefix + "\"success\":");
            writer.write(JsonHelper.convert(this.success));
            prefix = ",";
        }
        
        if (!(this.async == null))
        {
            writer.write(prefix + "\"async\":");
            writer.write(JsonHelper.convert(this.async));
            prefix = ",";
        }
        
        if (!(this.dependencySource == 0))
        {
            writer.write(prefix + "\"dependencySource\":");
            writer.write(JsonHelper.convert(this.dependencySource));
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
