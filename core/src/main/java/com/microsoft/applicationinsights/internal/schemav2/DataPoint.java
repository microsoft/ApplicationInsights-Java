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
 * Generated from DataPoint.bond (https://github.com/Microsoft/bond)
*/
package com.microsoft.applicationinsights.internal.schemav2;
import java.io.IOException;
import java.util.HashMap;
import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.google.common.base.Preconditions;

/**
 * Data contract class DataPoint.
 */
public class DataPoint implements
    IJsonSerializable
{
    /**
     * Backing field for property Name.
     */
    private String name;
    
    /**
     * Backing field for property Kind.
     */
    private int kind = DataPointType.Measurement;
    
    /**
     * Backing field for property Value.
     */
    private double value;
    
    /**
     * Backing field for property Count.
     */
    private Integer count;
    
    /**
     * Backing field for property Min.
     */
    private Double min;
    
    /**
     * Backing field for property Max.
     */
    private Double max;
    
    /**
     * Backing field for property StdDev.
     */
    private Double stdDev;
    
    /**
     * Initializes a new instance of the DataPoint class.
     */
    public DataPoint()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the Name property.
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Sets the Name property.
     */
    public void setName(String value) {
        this.name = value;
    }
    
    /**
     * Gets the Kind property.
     */
    public int getKind() {
        return this.kind;
    }
    
    /**
     * Sets the Kind property.
     */
    public void setKind(int value) {
        this.kind = value;
    }
    
    /**
     * Gets the Value property.
     */
    public double getValue() {
        return this.value;
    }
    
    /**
     * Sets the Value property.
     */
    public void setValue(double value) {
        this.value = value;
    }
    
    /**
     * Gets the Count property.
     */
    public Integer getCount() {
        return this.count;
    }
    
    /**
     * Sets the Count property.
     */
    public void setCount(Integer value) {
        this.count = value;
    }
    
    /**
     * Gets the Min property.
     */
    public Double getMin() {
        return this.min;
    }
    
    /**
     * Sets the Min property.
     */
    public void setMin(Double value) {
        this.min = value;
    }
    
    /**
     * Gets the Max property.
     */
    public Double getMax() {
        return this.max;
    }
    
    /**
     * Sets the Max property.
     */
    public void setMax(Double value) {
        this.max = value;
    }
    
    /**
     * Gets the StdDev property.
     */
    public Double getStdDev() {
        return this.stdDev;
    }
    
    /**
     * Sets the StdDev property.
     */
    public void setStdDev(Double value) {
        this.stdDev = value;
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
        writer.write("name", name);
        
        writer.write("kind", kind);
        writer.write("value", value);
        
        writer.write("count", count);
        writer.write("min", min);
        writer.write("max", max);
        writer.write("stdDev", stdDev);
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
