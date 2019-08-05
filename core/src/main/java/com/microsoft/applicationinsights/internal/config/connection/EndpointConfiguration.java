package com.microsoft.applicationinsights.internal.config.connection;

import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;

import java.net.URI;
import java.net.URISyntaxException;

public class EndpointConfiguration {
    private URI ingestionEndpoint;
    private URI liveEndpoint;
    private URI profilerEndpoint;
    private URI snapshotEndpoint; // TODO is this one needed?

    public EndpointConfiguration() {
        try {
            ingestionEndpoint = new URI(Defaults.INGESTION_ENDPOINT);
            liveEndpoint = new URI(Defaults.LIVE_ENDPOINT);
            profilerEndpoint = new URI(Defaults.PROFILER_ENDPOINT);
            snapshotEndpoint = new URI(Defaults.SNAPSHOT_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new RuntimeException("The ConnectionString.Defaults are invalid", e);
        }
    }

    public URI getIngestionEndpoint() {
        return ingestionEndpoint;
    }

    void setIngestionEndpoint(URI ingestionEndpoint) {
        this.ingestionEndpoint = ingestionEndpoint;
    }

    public URI getLiveEndpoint() {
        return liveEndpoint;
    }

    void setLiveEndpoint(URI liveEndpoint) {
        this.liveEndpoint = liveEndpoint;
    }

    public URI getProfilerEndpoint() {
        return profilerEndpoint;
    }

    void setProfilerEndpoint(URI profilerEndpoint) {
        this.profilerEndpoint = profilerEndpoint;
    }

    public URI getSnapshotEndpoint() {
        return snapshotEndpoint;
    }

    void setSnapshotEndpoint(URI snapshotEndpoint) {
        this.snapshotEndpoint = snapshotEndpoint;
    }
}
