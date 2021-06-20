package com.microsoft.applicationinsights.internal.config.connection;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;

import java.net.MalformedURLException;
import java.net.URL;

public class EndpointProvider {
    @VisibleForTesting static final String INGESTION_URL_PATH = "v2.1/track";
    @VisibleForTesting static final String LIVE_URL_PATH = "QuickPulseService.svc";
    @VisibleForTesting static final String API_PROFILES_APP_ID_URL_PREFIX = "api/profiles/"; // <base uri, with host>/api/profiles/<ikey>/appid
    @VisibleForTesting static final String API_PROFILES_APP_ID_URL_SUFFIX = "/appId";

    private volatile URL ingestionEndpoint;
    private volatile URL ingestionEndpointUrl;
    private volatile URL liveEndpointUrl;
    private volatile URL profilerEndpoint;
    private volatile URL snapshotEndpoint;

    public EndpointProvider() {
        try {
            ingestionEndpoint = new URL(Defaults.INGESTION_ENDPOINT);
            ingestionEndpointUrl = buildIngestionUrl(ingestionEndpoint);
            liveEndpointUrl = buildLiveUri(new URL(Defaults.LIVE_ENDPOINT));
            profilerEndpoint = new URL(Defaults.PROFILER_ENDPOINT);
            snapshotEndpoint = new URL(Defaults.SNAPSHOT_ENDPOINT);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("ConnectionString.Defaults are invalid", e);
        }
    }

    private URL buildIngestionUrl(URL baseUri) throws MalformedURLException {
        return buildUrl(baseUri, INGESTION_URL_PATH);
    }

    private URL buildLiveUri(URL baseUri) throws MalformedURLException {
        return buildUrl(baseUri, LIVE_URL_PATH);
    }

    public URL getIngestionEndpointUrl() {
        return ingestionEndpointUrl;
    }

    public synchronized URL getAppIdEndpointUrl(String instrumentationKey) {
        return buildAppIdUri(instrumentationKey);
    }

    private URL buildAppIdUri(String instrumentationKey) {
        try {
            return buildUrl(ingestionEndpoint, API_PROFILES_APP_ID_URL_PREFIX +instrumentationKey+ API_PROFILES_APP_ID_URL_SUFFIX);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid instrumentationKey: "+instrumentationKey);
        }
    }

    URL buildUrl(URL baseUri, String appendPath) throws MalformedURLException {
        String uriString = baseUri.toString();
        if (!uriString.endsWith("/")) {
            uriString = uriString + "/";
        }

        if (appendPath.startsWith("/")) {
            appendPath = appendPath.substring(1);
        }

        return new URL(uriString + appendPath);
    }

    public URL getIngestionEndpoint() {
        return ingestionEndpoint;
    }

    void setIngestionEndpoint(URL ingestionEndpoint) {
        try {
            this.ingestionEndpointUrl = buildIngestionUrl(ingestionEndpoint);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not construct ingestion endpoint uri", e);
        }
        this.ingestionEndpoint = ingestionEndpoint;
    }

    public URL getLiveEndpointUrl() {
        return liveEndpointUrl;
    }

    void setLiveEndpoint(URL liveEndpoint) {
        try {
            this.liveEndpointUrl = buildLiveUri(liveEndpoint);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("could not construct live endpoint uri", e);
        }
    }

    public URL getProfilerEndpoint() {
        return profilerEndpoint;
    }

    void setProfilerEndpoint(URL profilerEndpoint) {
        this.profilerEndpoint = profilerEndpoint;
    }

    public URL getSnapshotEndpoint() {
        return snapshotEndpoint;
    }

    void setSnapshotEndpoint(URL snapshotEndpoint) {
        this.snapshotEndpoint = snapshotEndpoint;
    }
}
