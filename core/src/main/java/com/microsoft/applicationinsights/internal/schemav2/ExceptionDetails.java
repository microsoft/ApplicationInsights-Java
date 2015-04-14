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
     * Initializes a new instance of the class.
     */
    public ExceptionDetails()
    {
        this.InitializeFields();
    }

    public int getId() {
        return this.id;
    }

    public void setId(int value) {
        this.id = value;
    }

    public int getOuterId() {
        return this.outerId;
    }

    public void setOuterId(int value) {
        this.outerId = value;
    }

    public String getTypeName() {
        return this.typeName;
    }

    public void setTypeName(String value) {
        this.typeName = value;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String value) {
        this.message = value;
    }

    public boolean getHasFullStack() {
        return this.hasFullStack;
    }

    public void setHasFullStack(boolean value) {
        this.hasFullStack = value;
    }

    public String getStack() {
        return this.stack;
    }

    public void setStack(String value) {
        this.stack = value;
    }

    public ArrayList<StackFrame> getParsedStack() {
        if (this.parsedStack == null) {
            this.parsedStack = new ArrayList<StackFrame>();
        }
        return this.parsedStack;
    }

    public void setParsedStack(ArrayList<StackFrame> value) {
        this.parsedStack = value;
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        this.serializeContent(writer);
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("id", id);
        writer.write("outerId", outerId);
        writer.write("typeName", typeName);
        writer.write("message", message);
        writer.write("hasFullStack", hasFullStack);
        writer.write("stack", stack);
        writer.write("parsedStack", parsedStack);
    }

    protected void InitializeFields() {
    }
}
