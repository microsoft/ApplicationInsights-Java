package com.microsoft.applicationinsights.springwebapptest;

import java.util.ArrayList;

/**
 * Created by moralt on 05/05/2015.
 */
public class RequestTelemetryItem extends TelemetryItem {
    /**
     * Initializes a new TelemetryItem object
     *
     * @param docType The document type of the telemetry item
     * @param id      The ID of this object
     */
    public RequestTelemetryItem(DocumentType docType, String id) {
        super(docType, id);
    }

    @Override
    protected void initDefaultPropertiesToCompare() {
        defaultPropertiesToCompare = new ArrayList<String>();
        defaultPropertiesToCompare.add("port");
        defaultPropertiesToCompare.add("responseCode");
        defaultPropertiesToCompare.add("uri");
    }
}
