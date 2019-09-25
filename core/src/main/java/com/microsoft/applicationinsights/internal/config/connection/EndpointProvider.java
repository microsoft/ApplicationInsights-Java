package com.microsoft.applicationinsights.internal.config.connection;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

public class EndpointProvider {
    @VisibleForTesting static final String INGESTION_URI_PATH = "v2/track";
    @VisibleForTesting static final String LIVE_URI_PATH = "QuickPulseService.svc";
    @VisibleForTesting static final String API_PROFILES_APP_ID_URI_PREFIX = "api/profiles/"; // <base uri, with host>/api/profiles/<ikey>/appid
    @VisibleForTesting static final String API_PROFILES_APP_ID_URI_SUFFIX = "/appId";

    private volatile URI ingestionEndpoint;
    private volatile URI ingestionEndpointURL;
    private volatile URI liveEndpointURL;
    private volatile URI profilerEndpoint;
    private volatile URI snapshotEndpoint;

    public EndpointProvider() {
        try {
            ingestionEndpoint = new URI(Defaults.INGESTION_ENDPOINT);
            ingestionEndpointURL = buildIngestionUri(ingestionEndpoint);
            liveEndpointURL = buildLiveUri(new URI(Defaults.LIVE_ENDPOINT));
            profilerEndpoint = new URI(Defaults.PROFILER_ENDPOINT);
            snapshotEndpoint = new URI(Defaults.SNAPSHOT_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("ConnectionString.Defaults are invalid", e);
        }
    }

    private URI buildIngestionUri(URI baseUri) throws URISyntaxException {
        return new URIBuilder(baseUri).setPath(INGESTION_URI_PATH).build();
    }

    private URI buildLiveUri(URI baseUri) throws URISyntaxException {
        return new URIBuilder(baseUri).setPath(LIVE_URI_PATH).build();
    }

    public URI getIngestionEndpointURL() {
        return ingestionEndpointURL;
    }

    public URI getAppIdEndpointURL(String instrumentationKey) {
        try {
            return new URIBuilder(ingestionEndpoint).setPath(API_PROFILES_APP_ID_URI_PREFIX +instrumentationKey+ API_PROFILES_APP_ID_URI_SUFFIX).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid instrumentationKey: "+instrumentationKey);
        }
    }

    public URI getIngestionEndpoint() {
        return ingestionEndpoint;
    }

    void setIngestionEndpoint(URI ingestionEndpoint) {
        try {
            this.ingestionEndpointURL = buildIngestionUri(ingestionEndpoint);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not construct ingestion endpoint uri", e);
        }
        this.ingestionEndpoint = ingestionEndpoint;
    }

    public URI getLiveEndpointURL() {
        return liveEndpointURL;
    }

    void setLiveEndpoint(URI liveEndpoint) {
        try {
            this.liveEndpointURL = buildLiveUri(liveEndpoint);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("could not construct live endpoint uri", e);
        }
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
