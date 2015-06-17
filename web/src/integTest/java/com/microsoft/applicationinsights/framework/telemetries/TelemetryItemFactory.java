package com.microsoft.applicationinsights.framework.telemetries;

import org.json.JSONObject;

/**
 * Created by amnonsh on 5/29/2015.
 */
public enum TelemetryItemFactory {
    INSTANCE;

    TelemetryItemFactory() {
    }

    public static TelemetryItem createTelemetryItem(DocumentType docType, JSONObject json) throws Exception {
        switch (docType) {
            case Requests:
                return new RequestTelemetryItem(json);
            case PerformanceCounters:
                return new PerformanceCounterTelemetryItem(json);
            default:
                throw new Exception("Unsupported document type: " + docType.toString());
        }
    }
}
