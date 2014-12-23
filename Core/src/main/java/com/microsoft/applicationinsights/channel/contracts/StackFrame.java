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
 * Data contract class StackFrame.
 */
public class StackFrame implements
    IJsonSerializable
{
    /**
     * Backing field for property Level.
     */
    private int level;
    
    /**
     * Backing field for property Method.
     */
    private String method;
    
    /**
     * Backing field for property Assembly.
     */
    private String assembly;
    
    /**
     * Backing field for property FileName.
     */
    private String fileName;
    
    /**
     * Backing field for property Line.
     */
    private int line;
    
    /**
     * Initializes a new instance of the <see cref="StackFrame"/> class.
     */
    public StackFrame()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the Level property.
     */
    public int getLevel() {
        return this.level;
    }
    
    /**
     * Sets the Level property.
     */
    public void setLevel(int value) {
        this.level = value;
    }
    
    /**
     * Gets the Method property.
     */
    public String getMethod() {
        return this.method;
    }
    
    /**
     * Sets the Method property.
     */
    public void setMethod(String value) {
        this.method = value;
    }
    
    /**
     * Gets the Assembly property.
     */
    public String getAssembly() {
        return this.assembly;
    }
    
    /**
     * Sets the Assembly property.
     */
    public void setAssembly(String value) {
        this.assembly = value;
    }
    
    /**
     * Gets the FileName property.
     */
    public String getFileName() {
        return this.fileName;
    }
    
    /**
     * Sets the FileName property.
     */
    public void setFileName(String value) {
        this.fileName = value;
    }
    
    /**
     * Gets the Line property.
     */
    public int getLine() {
        return this.line;
    }
    
    /**
     * Sets the Line property.
     */
    public void setLine(int value) {
        this.line = value;
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
        writer.write(prefix + "\"level\":");
        writer.write(JsonHelper.convert(this.level));
        prefix = ",";
        
        writer.write(prefix + "\"method\":");
        writer.write(JsonHelper.convert(this.method));
        prefix = ",";
        
        if (!(this.assembly == null))
        {
            writer.write(prefix + "\"assembly\":");
            writer.write(JsonHelper.convert(this.assembly));
            prefix = ",";
        }
        
        if (!(this.fileName == null))
        {
            writer.write(prefix + "\"fileName\":");
            writer.write(JsonHelper.convert(this.fileName));
            prefix = ",";
        }
        
        if (!(this.line == 0))
        {
            writer.write(prefix + "\"line\":");
            writer.write(JsonHelper.convert(this.line));
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
