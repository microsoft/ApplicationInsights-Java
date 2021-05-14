package com.microsoft.applicationinsights.internal.config.connection;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;

import java.net.URI;
import java.net.URISyntaxException;

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
    private volatile URI statsbeatEndpointUrl;

    public EndpointProvider() {
        try {
            ingestionEndpoint = new URI(Defaults.INGESTION_ENDPOINT);
            ingestionEndpointURL = buildIngestionUri(ingestionEndpoint);
            liveEndpointURL = buildLiveUri(new URI(Defaults.LIVE_ENDPOINT));
            profilerEndpoint = new URI(Defaults.PROFILER_ENDPOINT);
            snapshotEndpoint = new URI(Defaults.SNAPSHOT_ENDPOINT);
            statsbeatEndpointUrl = buildIngestionUri(ingestionEndpoint);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("ConnectionString.Defaults are invalid", e);
        }
    }

    private URI buildIngestionUri(URI baseUri) throws URISyntaxException {
        return buildUri(baseUri, INGESTION_URI_PATH);
    }

    private URI buildLiveUri(URI baseUri) throws URISyntaxException {
        return buildUri(baseUri, LIVE_URI_PATH);
    }

    public URI getIngestionEndpointURL() {
        return ingestionEndpointURL;
    }

    public URI getStatsbeatEndpointUrl() {
        return statsbeatEndpointUrl;
    }

    public synchronized URI getAppIdEndpointURL(String instrumentationKey) {
        return buildAppIdUri(instrumentationKey);
    }

    private URI buildAppIdUri(String instrumentationKey) {
        try {
            return buildUri(ingestionEndpoint, API_PROFILES_APP_ID_URI_PREFIX +instrumentationKey+ API_PROFILES_APP_ID_URI_SUFFIX);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid instrumentationKey: "+instrumentationKey);
        }
    }

    URI buildUri(URI baseUri, String appendPath) throws URISyntaxException {
        String uriString = baseUri.toString();
        if (!uriString.endsWith("/")) {
            uriString = uriString + "/";
        }

        if (appendPath.startsWith("/")) {
            appendPath = appendPath.substring(1);
        }

        return new URI(uriString + appendPath);
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

    void setStatsbeatEndpoint(URI statsbeatEndpoint) {
        try {
            this.statsbeatEndpointUrl = buildIngestionUri(statsbeatEndpoint);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("could not construct statsbeat ingestion endpoint uri", e);
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
