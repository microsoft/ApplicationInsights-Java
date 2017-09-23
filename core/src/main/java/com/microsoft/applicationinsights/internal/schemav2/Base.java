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
/*
 * Generated from Base.bond (https://github.com/Microsoft/bond)
*/
package com.microsoft.applicationinsights.internal.schemav2;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import java.io.IOException;

/**
 * Data contract class Base.
 */
public class Base
    implements JsonSerializable
{
    /**
     * Backing field for property BaseType.
     */
    private String baseType;
    
    /**
     * Initializes a new instance of the Base class.
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
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException
    {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");
        this.serializeContent(writer);
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException
    {
        writer.writeRequired("baseType", baseType, 1000);
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
