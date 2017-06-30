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
 * Generated from PageViewPerfData.bond (https://github.com/Microsoft/bond)
*/
package com.microsoft.applicationinsights.internal.schemav2;
import java.io.IOException;
import java.util.HashMap;
import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.google.common.base.Preconditions;

/**
 * Data contract class PageViewPerfData.
 */
public class PageViewPerfData extends PageViewData implements
    ITelemetryData
{
    /**
     * Backing field for property PerfTotal.
     */
    private String perfTotal;
    
    /**
     * Backing field for property NetworkConnect.
     */
    private String networkConnect;
    
    /**
     * Backing field for property SentRequest.
     */
    private String sentRequest;
    
    /**
     * Backing field for property ReceivedResponse.
     */
    private String receivedResponse;
    
    /**
     * Backing field for property DomProcessing.
     */
    private String domProcessing;
    
    /**
     * Initializes a new instance of the PageViewPerfData class.
     */
    public PageViewPerfData()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the PerfTotal property.
     */
    public String getPerfTotal() {
        return this.perfTotal;
    }
    
    /**
     * Sets the PerfTotal property.
     */
    public void setPerfTotal(String value) {
        this.perfTotal = value;
    }
    
    /**
     * Gets the NetworkConnect property.
     */
    public String getNetworkConnect() {
        return this.networkConnect;
    }
    
    /**
     * Sets the NetworkConnect property.
     */
    public void setNetworkConnect(String value) {
        this.networkConnect = value;
    }
    
    /**
     * Gets the SentRequest property.
     */
    public String getSentRequest() {
        return this.sentRequest;
    }
    
    /**
     * Sets the SentRequest property.
     */
    public void setSentRequest(String value) {
        this.sentRequest = value;
    }
    
    /**
     * Gets the ReceivedResponse property.
     */
    public String getReceivedResponse() {
        return this.receivedResponse;
    }
    
    /**
     * Sets the ReceivedResponse property.
     */
    public void setReceivedResponse(String value) {
        this.receivedResponse = value;
    }
    
    /**
     * Gets the DomProcessing property.
     */
    public String getDomProcessing() {
        return this.domProcessing;
    }
    
    /**
     * Sets the DomProcessing property.
     */
    public void setDomProcessing(String value) {
        this.domProcessing = value;
    }
    

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException
    {
        super.serializeContent(writer);
        writer.write("perfTotal", perfTotal);
        writer.write("networkConnect", networkConnect);
        writer.write("sentRequest", sentRequest);
        writer.write("receivedResponse", receivedResponse);
        writer.write("domProcessing", domProcessing);
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
