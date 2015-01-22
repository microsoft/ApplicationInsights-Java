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
    public static final String EnvelopeName = "Microsoft.ApplicationInsights.PageViewPerf";

    /**
     * Base Type for this telemetry.
     */
    public static final String BaseType = "Microsoft.ApplicationInsights.PageViewPerfData";

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

    protected void InitializeFields() {
    }
}
