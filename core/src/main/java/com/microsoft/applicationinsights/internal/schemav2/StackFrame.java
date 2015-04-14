/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
