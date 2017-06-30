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
import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.google.common.base.Preconditions;

/**
 * Data contract class User.
 */
public class User implements
    IJsonSerializable
{
    /**
     * Backing field for property AccountId.
     */
    private String accountId;
    
    /**
     * Backing field for property UserAgent.
     */
    private String userAgent;
    
    /**
     * Backing field for property Id.
     */
    private String id;
    
    /**
     * Backing field for property AuthUserId.
     */
    private String authUserId;
    
    /**
     * Initializes a new instance of the User class.
     */
    public User()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the AccountId property.
     */
    public String getAccountId() {
        return this.accountId;
    }
    
    /**
     * Sets the AccountId property.
     */
    public void setAccountId(String value) {
        this.accountId = value;
    }
    
    /**
     * Gets the UserAgent property.
     */
    public String getUserAgent() {
        return this.userAgent;
    }
    
    /**
     * Sets the UserAgent property.
     */
    public void setUserAgent(String value) {
        this.userAgent = value;
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
     * Gets the AuthUserId property.
     */
    public String getAuthUserId() {
        return this.authUserId;
    }
    
    /**
     * Sets the AuthUserId property.
     */
    public void setAuthUserId(String value) {
        this.authUserId = value;
    }
    

    /**
     * Adds all members of this class to a hashmap
     * @param map to which the members of this class will be added.
     */
    public void addToHashMap(HashMap<String, String> map)
    {
        if (this.accountId != null) {
            map.put("accountId", this.accountId);
        }
        if (this.userAgent != null) {
            map.put("userAgent", this.userAgent);
        }
        if (this.id != null) {
            map.put("id", this.id);
        }
        if (this.authUserId != null) {
            map.put("authUserId", this.authUserId);
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
        writer.write("accountId", accountId);
        writer.write("userAgent", userAgent);
        writer.write("id", id);
        writer.write("authUserId", authUserId);
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
