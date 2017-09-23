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
 * Generated from MessageData.bond (https://github.com/Microsoft/bond)
*/
package com.microsoft.applicationinsights.internal.schemav2;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Data contract class MessageData.
 */
public class MessageData extends Domain
{
    /**
     * Backing field for property Ver.
     */
    private int ver = 2;
    
    /**
     * Backing field for property Message.
     */
    private String message;
    
    /**
     * Backing field for property SeverityLevel.
     */
    private SeverityLevel severityLevel;
    
    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;
    
    /**
     * Initializes a new instance of the MessageData class.
     */
    public MessageData()
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
     * Gets the SeverityLevel property.
     */
    public SeverityLevel getSeverityLevel() {
        return this.severityLevel;
    }
    
    /**
     * Sets the SeverityLevel property.
     */
    public void setSeverityLevel(SeverityLevel value) {
        this.severityLevel = value;
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
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException
    {
        super.serializeContent(writer);
        writer.write("ver", ver);
        writer.writeRequired("message", message, 32768);
        writer.write("severityLevel", severityLevel);
        writer.write("properties", properties);
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
