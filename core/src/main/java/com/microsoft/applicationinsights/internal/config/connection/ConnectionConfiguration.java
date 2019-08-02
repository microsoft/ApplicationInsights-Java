package com.microsoft.applicationinsights.internal.config.connection;

import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;

public class ConnectionConfiguration {
    private String instrumentationKey; // could be a different authorization object in the future
    private String ingestionEndpoint;
    private String liveEndpoint;
    private String profilerEndpoint;
    private String snapshotEndpoint; // TODO is this one needed?

    public ConnectionConfiguration() {
        ingestionEndpoint = Defaults.INGESTION_ENDPOINT;
        liveEndpoint = Defaults.LIVE_ENDPOINT;
        profilerEndpoint = Defaults.PROFILER_ENDPOINT;
        snapshotEndpoint = Defaults.SNAPSHOT_ENDPOINT;
    }

    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    void setInstrumentationKey(String instrumentationKey) {
        this.instrumentationKey = instrumentationKey;
    }

    public String getIngestionEndpoint() {
        return ingestionEndpoint;
    }

    void setIngestionEndpoint(String ingestionEndpoint) {
        this.ingestionEndpoint = ingestionEndpoint;
    }

    public String getLiveEndpoint() {
        return liveEndpoint;
    }

    void setLiveEndpoint(String liveEndpoint) {
        this.liveEndpoint = liveEndpoint;
    }

    public String getProfilerEndpoint() {
        return profilerEndpoint;
    }

    void setProfilerEndpoint(String profilerEndpoint) {
        this.profilerEndpoint = profilerEndpoint;
    }

    public String getSnapshotEndpoint() {
        return snapshotEndpoint;
    }

    void setSnapshotEndpoint(String snapshotEndpoint) {
        this.snapshotEndpoint = snapshotEndpoint;
    }
}
