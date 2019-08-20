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
    @VisibleForTesting static final String API_PROFILES_APP_ID_URI_PREFIX = "api/profiles/";
    @VisibleForTesting static final String API_PROFILES_APP_ID_URI_SUFFIX = "/appId";

    private final AtomicReference<URI> ingestionEndpoint = new AtomicReference<>();
    private final AtomicReference<URI> ingestionEndpointURL = new AtomicReference<>();
    private final AtomicReference<URI> liveEndpointURL = new AtomicReference<>();
    private final AtomicReference<URI> profilerEndpoint = new AtomicReference<>();
    private final AtomicReference<URI> snapshotEndpoint = new AtomicReference<>(); // TODO is this one needed?

    public EndpointProvider() {
        try {
            ingestionEndpoint.set(new URI(Defaults.INGESTION_ENDPOINT));
            ingestionEndpointURL.set(buildIngestionUri(ingestionEndpoint.get()));
            liveEndpointURL.set(buildLiveUri(new URI(Defaults.LIVE_ENDPOINT)));
            profilerEndpoint.set(new URI(Defaults.PROFILER_ENDPOINT));
            snapshotEndpoint.set(new URI(Defaults.SNAPSHOT_ENDPOINT));
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
        return ingestionEndpointURL.get();
    }

    public URI getAppIdEndpointURL(String instrumentationKey) {
        try {
            return new URIBuilder(ingestionEndpoint.get()).setPath(API_PROFILES_APP_ID_URI_PREFIX +instrumentationKey+ API_PROFILES_APP_ID_URI_SUFFIX).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid instrumentationKey: "+instrumentationKey);
        }
    }

    public URI getIngestionEndpoint() {
        return ingestionEndpoint.get();
    }

    void setIngestionEndpoint(URI ingestionEndpoint) {
        try {
            this.ingestionEndpointURL.set(buildIngestionUri(ingestionEndpoint));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not construct ingestion endpoint uri", e);
        }
        this.ingestionEndpoint.set(ingestionEndpoint);
    }

    public URI getLiveEndpointURL() {
        return liveEndpointURL.get();
    }

    void setLiveEndpoint(URI liveEndpoint) {
        try {
            this.liveEndpointURL.set(buildLiveUri(liveEndpoint));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("could not construct live endpoint uri", e);
        }
    }

    public URI getProfilerEndpoint() {
        return profilerEndpoint.get();
    }

    void setProfilerEndpoint(URI profilerEndpoint) {
        this.profilerEndpoint.set(profilerEndpoint);
    }

    public URI getSnapshotEndpoint() {
        return snapshotEndpoint.get();
    }

    void setSnapshotEndpoint(URI snapshotEndpoint) {
        this.snapshotEndpoint.set(snapshotEndpoint);
    }
}
