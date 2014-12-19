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
 * Data contract class Base.
 */
public class Base implements
    IJsonSerializable
{
    /**
     * Backing field for property BaseType.
     */
    private String baseType;
    
    /**
     * Initializes a new instance of the <see cref="Base"/> class.
     */
    public Base()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the BaseType property.
     */
    public String getBaseType() {
        return this.baseType;
    }
    
    /**
     * Sets the BaseType property.
     */
    public void setBaseType(String value) {
        this.baseType = value;
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
        if (!(this.baseType == null))
        {
            writer.write(prefix + "\"baseType\":");
            writer.write(JsonHelper.convert(this.baseType));
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
