package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class StackFrame.
 */
public class StackFrame implements JsonSerializable {
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
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        this.serializeContent(writer);
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("level", level);
        writer.write("method", method);
        writer.write("assembly", assembly);
        writer.write("fileName", fileName);
        writer.write("line", line);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {

    }
}
