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
     * Initializes a new instance of the class.
     */
    public StackFrame()
    {
        this.InitializeFields();
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int value) {
        this.level = value;
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(String value) {
        this.method = value;
    }

    public String getAssembly() {
        return this.assembly;
    }

    public void setAssembly(String value) {
        this.assembly = value;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String value) {
        this.fileName = value;
    }

    public int getLine() {
        return this.line;
    }

    public void setLine(int value) {
        this.line = value;
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be thrown during serialization.
     */
    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        this.serializeContent(writer);
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("level", level);
        writer.write("method", method);
        writer.write("assembly", assembly);
        writer.write("fileName", fileName);
        writer.write("line", line);
    }

    protected void InitializeFields() {
    }
}
