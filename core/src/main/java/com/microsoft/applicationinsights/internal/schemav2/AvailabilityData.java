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
 * Generated from AvailabilityData.bond (https://github.com/Microsoft/bond)
*/
package com.microsoft.applicationinsights.internal.schemav2;
import java.io.IOException;
import java.util.HashMap;
import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.google.common.base.Preconditions;

/**
 * Data contract class AvailabilityData.
 */
public class AvailabilityData extends Domain implements
    ITelemetryData
{
    /**
     * Backing field for property Ver.
     */
    private int ver = 2;
    
    /**
     * Backing field for property Id.
     */
    private String id;
    
    /**
     * Backing field for property Name.
     */
    private String name;
    
    /**
     * Backing field for property Duration.
     */
    private String duration;
    
    /**
     * Backing field for property Success.
     */
    private boolean success;
    
    /**
     * Backing field for property RunLocation.
     */
    private String runLocation;
    
    /**
     * Backing field for property Message.
     */
    private String message;
    
    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;
    
    /**
     * Backing field for property Measurements.
     */
    private ConcurrentMap<String, Double> measurements;
    
    /**
     * Initializes a new instance of the AvailabilityData class.
     */
    public AvailabilityData()
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
     * Gets the Id property.
     */
    public String getId() {
        return this.id;
    }
    
    /**
     * Sets the Id property.
     */
    public void setId(String value) {
        this.id = value;
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
     * Gets the Duration property.
     */
    public String getDuration() {
        return this.duration;
    }
    
    /**
     * Sets the Duration property.
     */
    public void setDuration(String value) {
        this.duration = value;
    }
    
    /**
     * Gets the Success property.
     */
    public boolean getSuccess() {
        return this.success;
    }
    
    /**
     * Sets the Success property.
     */
    public void setSuccess(boolean value) {
        this.success = value;
    }
    
    /**
     * Gets the RunLocation property.
     */
    public String getRunLocation() {
        return this.runLocation;
    }
    
    /**
     * Sets the RunLocation property.
     */
    public void setRunLocation(String value) {
        this.runLocation = value;
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
     * Gets the Properties property.
     */
    public ConcurrentMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new ConcurrentHashMap<String, String>();
        }
        return this.properties;
    }
    
    /**
     * Sets the Properties property.
     */
    public void setProperties(ConcurrentMap<String, String> value) {
        this.properties = value;
    }
    
    /**
     * Gets the Measurements property.
     */
    public ConcurrentMap<String, Double> getMeasurements() {
        if (this.measurements == null) {
            this.measurements = new ConcurrentHashMap<String, Double>();
        }
        return this.measurements;
    }
    
    /**
     * Sets the Measurements property.
     */
    public void setMeasurements(ConcurrentMap<String, Double> value) {
        this.measurements = value;
    }
    

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException
    {
        super.serializeContent(writer);
        writer.write("ver", ver);
        
        writer.write("id", id);
        
        writer.write("name", name);
        
        writer.write("duration", duration);
        
        writer.write("success", success);
        
        writer.write("runLocation", runLocation);
        writer.write("message", message);
        writer.write("properties", properties);
        writer.write("measurements", measurements);
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
