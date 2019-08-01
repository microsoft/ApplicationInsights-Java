package com.microsoft.applicationinsights.internal.config;

import com.microsoft.applicationinsights.internal.config.connection.ConnectionString;

public class ConnectionConfiguration {
    private String instrumentationKey; // could be a different authorization object in the future
    private String ingestionEndpoint;
    private String liveEndpoint;

    public ConnectionConfiguration() {
        ingestionEndpoint = ConnectionString.Defaults.INGESTION_ENDPOINT;
        liveEndpoint = ConnectionString.Defaults.LIVE_ENDPOINT;
    }

    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    public void setInstrumentationKey(String instrumentationKey) {
        this.instrumentationKey = instrumentationKey;
    }

    public String getIngestionEndpoint() {
        return ingestionEndpoint;
    }

    public void setIngestionEndpoint(String ingestionEndpoint) {
        this.ingestionEndpoint = ingestionEndpoint;
    }

    public String getLiveEndpoint() {
        return liveEndpoint;
    }

    public void setLiveEndpoint(String liveEndpoint) {
        this.liveEndpoint = liveEndpoint;
    }

}
