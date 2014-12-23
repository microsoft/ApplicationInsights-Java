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
 * Data contract class ExceptionDetails.
 */
public class ExceptionDetails implements
    IJsonSerializable
{
    /**
     * Backing field for property Id.
     */
    private int id;
    
    /**
     * Backing field for property OuterId.
     */
    private int outerId;
    
    /**
     * Backing field for property TypeName.
     */
    private String typeName;
    
    /**
     * Backing field for property Message.
     */
    private String message;
    
    /**
     * Backing field for property HasFullStack.
     */
    private boolean hasFullStack = true;
    
    /**
     * Backing field for property Stack.
     */
    private String stack;
    
    /**
     * Backing field for property ParsedStack.
     */
    private ArrayList<StackFrame> parsedStack;
    
    /**
     * Initializes a new instance of the <see cref="ExceptionDetails"/> class.
     */
    public ExceptionDetails()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the Id property.
     */
    public int getId() {
        return this.id;
    }
    
    /**
     * Sets the Id property.
     */
    public void setId(int value) {
        this.id = value;
    }
    
    /**
     * Gets the OuterId property.
     */
    public int getOuterId() {
        return this.outerId;
    }
    
    /**
     * Sets the OuterId property.
     */
    public void setOuterId(int value) {
        this.outerId = value;
    }
    
    /**
     * Gets the TypeName property.
     */
    public String getTypeName() {
        return this.typeName;
    }
    
    /**
     * Sets the TypeName property.
     */
    public void setTypeName(String value) {
        this.typeName = value;
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
     * Gets the HasFullStack property.
     */
    public boolean getHasFullStack() {
        return this.hasFullStack;
    }
    
    /**
     * Sets the HasFullStack property.
     */
    public void setHasFullStack(boolean value) {
        this.hasFullStack = value;
    }
    
    /**
     * Gets the Stack property.
     */
    public String getStack() {
        return this.stack;
    }
    
    /**
     * Sets the Stack property.
     */
    public void setStack(String value) {
        this.stack = value;
    }
    
    /**
     * Gets the ParsedStack property.
     */
    public ArrayList<StackFrame> getParsedStack() {
        if (this.parsedStack == null) {
            this.parsedStack = new ArrayList<StackFrame>();
        }
        return this.parsedStack;
    }
    
    /**
     * Sets the ParsedStack property.
     */
    public void setParsedStack(ArrayList<StackFrame> value) {
        this.parsedStack = value;
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
        if (!(this.id == 0))
        {
            writer.write(prefix + "\"id\":");
            writer.write(JsonHelper.convert(this.id));
            prefix = ",";
        }
        
        if (!(this.outerId == 0))
        {
            writer.write(prefix + "\"outerId\":");
            writer.write(JsonHelper.convert(this.outerId));
            prefix = ",";
        }
        
        writer.write(prefix + "\"typeName\":");
        writer.write(JsonHelper.convert(this.typeName));
        prefix = ",";
        
        writer.write(prefix + "\"message\":");
        writer.write(JsonHelper.convert(this.message));
        prefix = ",";
        
        if (!(this.hasFullStack == false))
        {
            writer.write(prefix + "\"hasFullStack\":");
            writer.write(JsonHelper.convert(this.hasFullStack));
            prefix = ",";
        }
        
        if (!(this.stack == null))
        {
            writer.write(prefix + "\"stack\":");
            writer.write(JsonHelper.convert(this.stack));
            prefix = ",";
        }
        
        if (!(this.parsedStack == null))
        {
            writer.write(prefix + "\"parsedStack\":");
            JsonHelper.writeList(writer, this.parsedStack);
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
