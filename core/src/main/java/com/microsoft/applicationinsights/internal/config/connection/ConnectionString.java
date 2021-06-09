package com.microsoft.applicationinsights.internal.config.connection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

public class ConnectionString {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionString.class);

    @VisibleForTesting
    static final int CONNECTION_STRING_MAX_LENGTH = 4096;

    private ConnectionString(){}

    public static void parseInto(String connectionString, TelemetryConfiguration targetConfig) throws InvalidConnectionStringException {
        if (connectionString.length() > CONNECTION_STRING_MAX_LENGTH) { // guard against malicious input
            throw new InvalidConnectionStringException("ConnectionString values with more than " + CONNECTION_STRING_MAX_LENGTH + " characters are not allowed.");
        }
        // parse key value pairs
        final Map<String, String> kvps;
        try {
            kvps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            kvps.putAll(Splitter.on(';').trimResults().omitEmptyStrings().withKeyValueSeparator('=').split(connectionString));
        } catch (IllegalArgumentException e) {
            throw new InvalidConnectionStringException("Could not parse connection string.");
        }

        mapToConnectionConfiguration(kvps, targetConfig);
    }

    private static void mapToConnectionConfiguration(Map<String, String> kvps, TelemetryConfiguration config) throws InvalidConnectionStringException {

        // get ikey
        String instrumentationKey = kvps.get(Keywords.INSTRUMENTATION_KEY);
        if (Strings.isNullOrEmpty(instrumentationKey)) {
            throw new InvalidConnectionStringException("Missing '"+Keywords.INSTRUMENTATION_KEY+"'");
        }
        if (!Strings.isNullOrEmpty(config.getInstrumentationKey())) {
            logger.warn("Connection string is overriding previously configured instrumentation key.");
        }
        config.setInstrumentationKey(instrumentationKey);

        // resolve suffix
        String suffix = kvps.get(Keywords.ENDPOINT_SUFFIX);
        if (!Strings.isNullOrEmpty(suffix)) {
            try {
                config.getEndpointProvider().setIngestionEndpoint(constructSecureEndpoint(EndpointPrefixes.INGESTION_ENDPOINT_PREFIX, suffix));
                config.getEndpointProvider().setLiveEndpoint(constructSecureEndpoint(EndpointPrefixes.LIVE_ENDPOINT_PREFIX, suffix));
                config.getEndpointProvider().setProfilerEndpoint(constructSecureEndpoint(EndpointPrefixes.PROFILER_ENDPOINT_PREFIX, suffix));
                config.getEndpointProvider().setSnapshotEndpoint(constructSecureEndpoint(EndpointPrefixes.SNAPSHOT_ENDPOINT_PREFIX, suffix));
            } catch (URISyntaxException e) {
                throw new InvalidConnectionStringException(Keywords.ENDPOINT_SUFFIX + " is invalid: " + suffix, e);
            }
        }

        // set explicit endpoints
        String liveEndpoint = kvps.get(Keywords.LIVE_ENDPOINT);
        if (!Strings.isNullOrEmpty(liveEndpoint)) {
            config.getEndpointProvider().setLiveEndpoint(toUriOrThrow(liveEndpoint, Keywords.LIVE_ENDPOINT));
        }

        String ingestionEndpoint = kvps.get(Keywords.INGESTION_ENDPOINT);
        if (!Strings.isNullOrEmpty(ingestionEndpoint)) {
            config.getEndpointProvider().setIngestionEndpoint(toUriOrThrow(ingestionEndpoint, Keywords.INGESTION_ENDPOINT));
        }

        String profilerEndpoint = kvps.get(Keywords.PROFILER_ENDPOINT);
        if (!Strings.isNullOrEmpty(profilerEndpoint)) {
            config.getEndpointProvider().setProfilerEndpoint(toUriOrThrow(profilerEndpoint, Keywords.PROFILER_ENDPOINT));
        }

        String snapshotEndpoint = kvps.get(Keywords.SNAPSHOT_ENDPOINT);
        if (!Strings.isNullOrEmpty(snapshotEndpoint)) {
            config.getEndpointProvider().setSnapshotEndpoint(toUriOrThrow(snapshotEndpoint, Keywords.SNAPSHOT_ENDPOINT));
        }
    }


    private static URI toUriOrThrow(String uri, String field) throws InvalidConnectionStringException {
        try {
            URI result = new URI(uri);
            final String scheme = result.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new InvalidConnectionStringException(field+" must specify supported protocol, either 'http' or 'https': \""+uri+"\"");
            }
            return result;
        } catch (URISyntaxException e) {
            throw new InvalidConnectionStringException(field + " is invalid: \"" + uri + "\"", e);
        }
    }

    @VisibleForTesting
    static URI constructSecureEndpoint(String prefix, String suffix) throws URISyntaxException {
        return new URI("https://" + StringUtils.strip(prefix, ".") + "." + StringUtils.strip(suffix, "."));
    }

    /**
     * All tokens are lowercase. Parsing should be case insensitive.
     */
    public static class Keywords {
        private Keywords(){}

        public static final String AUTHORIZATION = "Authorization";
        public static final String INSTRUMENTATION_KEY = "InstrumentationKey";
        public static final String ENDPOINT_SUFFIX = "EndpointSuffix";
        public static final String INGESTION_ENDPOINT = "IngestionEndpoint";
        public static final String LIVE_ENDPOINT = "LiveEndpoint";
        public static final String PROFILER_ENDPOINT = "ProfilerEndpoint";
        public static final String SNAPSHOT_ENDPOINT = "SnapshotEndpoint";
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

        public static final String INGESTION_ENDPOINT = "https://dc.services.visualstudio.com";
        public static final String LIVE_ENDPOINT =      "https://rt.services.visualstudio.com";
        public static final String PROFILER_ENDPOINT =  "https://agent.azureserviceprofiler.net";
        public static final String SNAPSHOT_ENDPOINT =  "https://agent.azureserviceprofiler.net";
    }
}
