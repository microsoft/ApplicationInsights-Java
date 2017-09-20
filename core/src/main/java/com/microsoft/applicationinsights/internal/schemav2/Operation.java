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
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.google.common.base.Preconditions;
import org.apache.http.annotation.Obsolete;

/**
 * Data contract class Operation.
 * This class is now obsolete and will be removed in coming versions. Please
 * avoid taking any dependencies on this class.
 */
@Obsolete
public class Operation
    implements JsonSerializable
{
    /**
     * Backing field for property Id.
     */
    private String id;
    
    /**
     * Backing field for property Name.
     */
    private String name;
    
    /**
     * Backing field for property ParentId.
     */
    private String parentId;
    
    /**
     * Backing field for property SyntheticSource.
     */
    private String syntheticSource;
    
    /**
     * Backing field for property CorrelationVector.
     */
    private String correlationVector;
    
    /**
     * Initializes a new instance of the Operation class.
     */
    public Operation()
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
     * Gets the ParentId property.
     */
    public String getParentId() {
        return this.parentId;
    }
    
    /**
     * Sets the ParentId property.
     */
    public void setParentId(String value) {
        this.parentId = value;
    }
    
    /**
     * Gets the SyntheticSource property.
     */
    public String getSyntheticSource() {
        return this.syntheticSource;
    }
    
    /**
     * Sets the SyntheticSource property.
     */
    public void setSyntheticSource(String value) {
        this.syntheticSource = value;
    }
    
    /**
     * Gets the CorrelationVector property.
     */
    public String getCorrelationVector() {
        return this.correlationVector;
    }
    
    /**
     * Sets the CorrelationVector property.
     */
    public void setCorrelationVector(String value) {
        this.correlationVector = value;
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
        if (this.name != null) {
            map.put("name", this.name);
        }
        if (this.parentId != null) {
            map.put("parentId", this.parentId);
        }
        if (this.syntheticSource != null) {
            map.put("syntheticSource", this.syntheticSource);
        }
        if (this.correlationVector != null) {
            map.put("correlationVector", this.correlationVector);
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
        writer.write("name", name);
        writer.write("parentId", parentId);
        writer.write("syntheticSource", syntheticSource);
        writer.write("correlationVector", correlationVector);
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
