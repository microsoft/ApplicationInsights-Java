/*
 * AppInsights-Java
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

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

/**
 * Data contract class PageViewPerfData.
 */
public class PageViewPerfData extends PageViewData {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String PAGE_VIEW_PERF_ENVELOPE_NAME = "Microsoft.ApplicationInsights.PageViewPerf";

    /**
     * Base Type for this telemetry.
     */
    public static final String PAGE_VIEW_PERF_BASE_TYPE = "Microsoft.ApplicationInsights.PageViewPerfData";

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
     * Initializes a new instance of the class.
     */
    public PageViewPerfData()
    {
        this.InitializeFields();
    }

    public String getPerfTotal() {
        return this.perfTotal;
    }

    public void setPerfTotal(String value) {
        this.perfTotal = value;
    }

    public String getNetworkConnect() {
        return this.networkConnect;
    }

    public void setNetworkConnect(String value) {
        this.networkConnect = value;
    }

    public String getSentRequest() {
        return this.sentRequest;
    }

    public void setSentRequest(String value) {
        this.sentRequest = value;
    }

    public String getReceivedResponse() {
        return this.receivedResponse;
    }

    public void setReceivedResponse(String value) {
        this.receivedResponse = value;
    }

    public String getDomProcessing() {
        return this.domProcessing;
    }

    public void setDomProcessing(String value) {
        this.domProcessing = value;
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be throw during serialization.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("perfTotal", perfTotal);
        writer.write("networkConnect", networkConnect);
        writer.write("sentRequest", sentRequest);
        writer.write("receivedResponse", receivedResponse);
        writer.write("domProcessing", domProcessing);
    }

    @Override
    public String getEnvelopName() {
        return PAGE_VIEW_PERF_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return PAGE_VIEW_PERF_BASE_TYPE;
    }

    protected void InitializeFields() {
    }
}
