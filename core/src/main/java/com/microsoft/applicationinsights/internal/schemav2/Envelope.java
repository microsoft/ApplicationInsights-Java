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
 * Generated from Envelope.bond (https://github.com/Microsoft/bond)
*/
package com.microsoft.applicationinsights.internal.schemav2;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.google.common.base.Preconditions;

/**
 * Data contract class Envelope.
 */
public class Envelope
    implements JsonSerializable
{
    /**
     * Backing field for property Ver.
     */
    private int ver = 1;
    
    /**
     * Backing field for property Name.
     */
    private String name;
    
    /**
     * Backing field for property Time.
     */
    private String time;
    
    /**
     * Backing field for property SampleRate.
     */
    private double sampleRate = 100.0;
    
    /**
     * Backing field for property Seq.
     */
    private String seq;
    
    /**
     * Backing field for property IKey.
     */
    private String iKey;
    
    /**
     * Backing field for property Tags.
     */
    private ConcurrentMap<String, String> tags;
    
    /**
     * Backing field for property Data.
     */
    private Base data;
    
    /**
     * Initializes a new instance of the Envelope class.
     */
    public Envelope()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the Ver property.
     */
    public int getVer() {
        return this.ver;
    }
    
    /**
     * Sets the Ver property.
     */
    public void setVer(int value) {
        this.ver = value;
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
     * Gets the Time property.
     */
    public String getTime() {
        return this.time;
    }
    
    /**
     * Sets the Time property.
     */
    public void setTime(String value) {
        this.time = value;
    }
    
    /**
     * Gets the SampleRate property.
     */
    public double getSampleRate() {
        return this.sampleRate;
    }
    
    /**
     * Sets the SampleRate property.
     */
    public void setSampleRate(double value) {
        this.sampleRate = value;
    }
    
    /**
     * Gets the Seq property.
     */
    public String getSeq() {
        return this.seq;
    }
    
    /**
     * Sets the Seq property.
     */
    public void setSeq(String value) {
        this.seq = value;
    }
    
    /**
     * Gets the IKey property.
     */
    public String getIKey() {
        return this.iKey;
    }
    
    /**
     * Sets the IKey property.
     */
    public void setIKey(String value) {
        this.iKey = value;
    }
    
    /**
     * Gets the Tags property.
     */
    public ConcurrentMap<String, String> getTags() {
        if (this.tags == null) {
            this.tags = new ConcurrentHashMap<String, String>();
        }
        return this.tags;
    }
    
    /**
     * Sets the Tags property.
     */
    public void setTags(ConcurrentMap<String, String> value) {
        this.tags = value;
    }
    
    /**
     * Gets the Data property.
     */
    public Base getData() {
        return this.data;
    }
    
    /**
     * Sets the Data property.
     */
    public void setData(Base value) {
        this.data = value;
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
        writer.write("ver", ver);
        writer.writeRequired("name", name, 1024);
        writer.writeRequired("time", time, 64);
        if (this.sampleRate > 0.0d) {
            writer.write("sampleRate", sampleRate);
        }
        writer.write("seq", seq, 64);
        writer.write("iKey", iKey, 40);
        writer.write("tags", tags);
        writer.write("data", data);
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
