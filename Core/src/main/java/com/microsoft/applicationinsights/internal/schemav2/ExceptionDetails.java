package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.ArrayList;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class ExceptionDetails.
 */
public class ExceptionDetails implements JsonSerializable {
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
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        this.serializeContent(writer);
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException
    {
        writer.write("id", id);
        writer.write("outerId", outerId);
        writer.write("typeName", typeName);
        writer.write("message", message);
        writer.write("hasFullStack", hasFullStack);
        writer.write("stack", stack);
        writer.write("parsedStack", parsedStack);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {

    }
}
