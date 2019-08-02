package com.microsoft.applicationinsights.internal.config.connection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.util.Map;
import java.util.TreeMap;

public class ConnectionString {
    private ConnectionString(){}

    public static void parseInto(String connectionString, ConnectionConfiguration targetConfig) throws ConnectionStringParseException {
        // parse key value pairs
        final Map<String, String> kvps;
        try {
            kvps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            kvps.putAll(Splitter.on(';').trimResults().omitEmptyStrings().withKeyValueSeparator('=').split(connectionString));
        } catch (IllegalArgumentException e) {
            throw new InvalidConnectionStringException(e);
        }

        mapToConnectionConfiguration(kvps, targetConfig);
    }

    @VisibleForTesting
    static void mapToConnectionConfiguration(Map<String, String> kvps, ConnectionConfiguration result) throws ConnectionStringParseException {
        // TODO validate values?

        // check for authorization
        String authorizationType = kvps.get(ConnectionStringKeys.AUTHORIZATION);
        if (!(Strings.isNullOrEmpty(authorizationType) || "ikey".equalsIgnoreCase(authorizationType))) {
            throw new UnsupportedAuthorizationTypeException(authorizationType + " is not a supported Authorization value. Supported values: [\"ikey\"].");
        }

        // get ikey
        String instrumentationKey = kvps.get(ConnectionStringKeys.INSTRUMENTATION_KEY);
        if (Strings.isNullOrEmpty(instrumentationKey)) {
            throw new InvalidConnectionStringException("Missing 'InstrumentationKey'");
        }
        result.setInstrumentationKey(instrumentationKey);

        // resolve suffix
        String suffix = kvps.get(ConnectionStringKeys.ENDPOINT_SUFFIX);
        if (!Strings.isNullOrEmpty(suffix)) {
            result.setIngestionEndpoint(constructSecureEndpoint(EndpointPrefixes.INGESTION_ENDPOINT_PREFIX, suffix));
            result.setLiveEndpoint(constructSecureEndpoint(EndpointPrefixes.LIVE_ENDPOINT_PREFIX, suffix));
            result.setProfilerEndpoint(constructSecureEndpoint(EndpointPrefixes.PROFILER_ENDPOINT_PREFIX, suffix));
            result.setSnapshotEndpoint(constructSecureEndpoint(EndpointPrefixes.SNAPSHOT_ENDPOINT_PREFIX, suffix));
        }

        // set explicit endpoints
        String liveEndpoint = kvps.get(ConnectionStringKeys.LIVE_ENDPOINT);
        if (!Strings.isNullOrEmpty(liveEndpoint)) {
            result.setLiveEndpoint(liveEndpoint);
        }

        String ingestionEndpoint = kvps.get(ConnectionStringKeys.INGESTION_ENDPOINT);
        if (!Strings.isNullOrEmpty(ingestionEndpoint)) {
            result.setIngestionEndpoint(ingestionEndpoint);
        }

        String profilerEndpoint = kvps.get(ConnectionStringKeys.PROFILER_ENDPOINT);
        if (!Strings.isNullOrEmpty(profilerEndpoint)) {
            result.setProfilerEndpoint(profilerEndpoint);
        }

        String snapshotEndpoint = kvps.get(ConnectionStringKeys.SNAPSHOT_ENDPOINT);
        if (!Strings.isNullOrEmpty(snapshotEndpoint)) {
            result.setSnapshotEndpoint(snapshotEndpoint);
        }

    }

    @VisibleForTesting
    static String constructSecureEndpoint(String prefix, String suffix) {
        return "https://" + prefix + "." + suffix;
    }

    /**
     * All tokens are lowercase. Parsing should be case insensitive.
     */
    @VisibleForTesting
    static class ConnectionStringKeys {
        private ConnectionStringKeys(){}

        static final String AUTHORIZATION = "Authorization";
        static final String INSTRUMENTATION_KEY = "InstrumentationKey";
        static final String ENDPOINT_SUFFIX = "EndpointSuffix";
        static final String INGESTION_ENDPOINT = "IngestionEndpoint";
        static final String LIVE_ENDPOINT = "LiveEndpoint";
        static final String PROFILER_ENDPOINT = "ProfilerEndpoint";
        static final String SNAPSHOT_ENDPOINT = "SnapshotEndpoint";
    }

    @VisibleForTesting
    static class EndpointPrefixes {
        private EndpointPrefixes(){}

        static final String INGESTION_ENDPOINT_PREFIX = "dc";
        static final String LIVE_ENDPOINT_PREFIX = "live";
        static final String PROFILER_ENDPOINT_PREFIX = "profiler";
        static final String SNAPSHOT_ENDPOINT_PREFIX = "snapshot";
    }

    public static class Defaults {
        private Defaults(){}

        public static final String INGESTION_ENDPOINT = "https://dc.services.visualstudio.com ";
        public static final String LIVE_ENDPOINT = "https://rt.services.visualstudio.com/";
        public static final String PROFILER_ENDPOINT = "https://agent.azureserviceprofiler.net ";
        public static final String SNAPSHOT_ENDPOINT = "https://agent.azureserviceprofiler.net "; // TODO verify if this is needed.
    }
}
