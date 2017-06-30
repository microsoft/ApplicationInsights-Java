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
 * Generated from ContextTagKeys.bond (https://github.com/Microsoft/bond)
*/
package com.microsoft.applicationinsights.internal.schemav2;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.google.common.base.Preconditions;

/**
 * Data contract class Device.
 */
public class Device
    implements JsonSerializable
{
    /**
     * Backing field for property Id.
     */
    private String id;
    
    /**
     * Backing field for property Locale.
     */
    private String locale;
    
    /**
     * Backing field for property Model.
     */
    private String model;
    
    /**
     * Backing field for property OemName.
     */
    private String oemName;
    
    /**
     * Backing field for property OsVersion.
     */
    private String osVersion;
    
    /**
     * Backing field for property Type.
     */
    private String type;
    
    /**
     * Initializes a new instance of the Device class.
     */
    public Device()
    {
        this.InitializeFields();
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
     * Gets the Locale property.
     */
    public String getLocale() {
        return this.locale;
    }
    
    /**
     * Sets the Locale property.
     */
    public void setLocale(String value) {
        this.locale = value;
    }
    
    /**
     * Gets the Model property.
     */
    public String getModel() {
        return this.model;
    }
    
    /**
     * Sets the Model property.
     */
    public void setModel(String value) {
        this.model = value;
    }
    
    /**
     * Gets the OemName property.
     */
    public String getOemName() {
        return this.oemName;
    }
    
    /**
     * Sets the OemName property.
     */
    public void setOemName(String value) {
        this.oemName = value;
    }
    
    /**
     * Gets the OsVersion property.
     */
    public String getOsVersion() {
        return this.osVersion;
    }
    
    /**
     * Sets the OsVersion property.
     */
    public void setOsVersion(String value) {
        this.osVersion = value;
    }
    
    /**
     * Gets the Type property.
     */
    public String getType() {
        return this.type;
    }
    
    /**
     * Sets the Type property.
     */
    public void setType(String value) {
        this.type = value;
    }
    

    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
    public void addToHashMap(HashMap<String, String> map)
    {
        if (this.id != null) {
            map.put("id", this.id);
        }
        if (this.locale != null) {
            map.put("locale", this.locale);
        }
        if (this.model != null) {
            map.put("model", this.model);
        }
        if (this.oemName != null) {
            map.put("oemName", this.oemName);
        }
        if (this.osVersion != null) {
            map.put("osVersion", this.osVersion);
        }
        if (this.type != null) {
            map.put("type", this.type);
        }
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
        writer.write("id", id);
        writer.write("locale", locale);
        writer.write("model", model);
        writer.write("oemName", oemName);
        writer.write("osVersion", osVersion);
        writer.write("type", type);
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
