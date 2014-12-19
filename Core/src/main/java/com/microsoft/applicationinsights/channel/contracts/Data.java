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
 * Data contract class Data.
 */
public class Data<TDomain extends ITelemetryData> extends Base implements
    ITelemetryData
{
    /**
     * Backing field for property BaseData.
     */
    private TDomain baseData;
    
    /**
     * Initializes a new instance of the <see cref="Data{TDomain}"/> class.
     */
    public Data()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the BaseData property.
     */
    public TDomain getBaseData() {
        return this.baseData;
    }
    
    /**
     * Sets the BaseData property.
     */
    public void setBaseData(TDomain value) {
        this.baseData = value;
    }
    

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected String serializeContent(Writer writer) throws IOException
    {
        String prefix = super.serializeContent(writer);
        writer.write(prefix + "\"baseData\":");
        JsonHelper.writeJsonSerializable(writer, this.baseData);
        prefix = ",";
        
        return prefix;
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
