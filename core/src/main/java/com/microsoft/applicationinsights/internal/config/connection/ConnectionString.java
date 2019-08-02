package com.microsoft.applicationinsights.internal.config.connection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.config.ConnectionConfiguration;

import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ConnectionString {
    private ConnectionString(){}

    @VisibleForTesting
    static class KeyValuePairScanner implements Iterator<Map.Entry<String, String>> {

        enum State {
            KEY,
            VALUE,
            FINISHED,
        }

        private final String input;
        private final char pairSeparator;
        private final char valueSeparator;

        private Map.Entry<String, String> cachedNext = null;
        private int startIndex = 0;
        private State state = State.KEY;

        KeyValuePairScanner(String input, char pairSeparator, char valueSeparator) {
            this.input = input;
            this.pairSeparator = pairSeparator;
            this.valueSeparator = valueSeparator;
        }

        @Override
        public boolean hasNext() {
            return cachedNext != null || cacheNext();
        }

        @Override
        public Map.Entry<String, String> next() {
            if (!hasNext()) {
                return null;
            }
            Map.Entry<String, String> result = cachedNext;
            cachedNext = null;
            return result;
        }

        /**
         *
         * @return true if an entry was cached.
         */
        private boolean cacheNext() {
            if (state == State.FINISHED) {
                return false;
            }

            // TODO handle error cases

            char separator = valueSeparator;
            int i = startIndex;
            for (; i < input.length(); i++) {
                if (input.charAt(i) == separator) {
                    if (state == State.KEY) {
                        cachedNext = new SimpleEntry<>(input.substring(startIndex, i), null);
                        startIndex = ++i; // skip separator
                        separator = pairSeparator;
                        state = State.VALUE;
                    } else if (state == State.VALUE) {
                        cachedNext.setValue(input.substring(startIndex, i));
                        startIndex = ++i; // skip separator
                        state = State.KEY;
                        break;
                    }
                }
            }

            if (i == input.length()) {
                cachedNext.setValue(input.substring(startIndex, i));
                state = State.FINISHED;
            }

            return true;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static ConnectionConfiguration parse(String connectionString) throws ConnectionStringParseException {
        // parse key value pairs
        final Map<String, String> kvps;
        try {
            kvps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            kvps.putAll(Splitter.on(';').trimResults().omitEmptyStrings().withKeyValueSeparator('=').split(connectionString));
        } catch (IllegalArgumentException e) {
            throw new InvalidConnectionStringException(e);
        }

        return mapToConnectionConfiguration(kvps);

    }

    @VisibleForTesting
    static ConnectionConfiguration mapToConnectionConfiguration(Map<String, String> kvps) throws ConnectionStringParseException {
        final ConnectionConfiguration result = new ConnectionConfiguration();

        // TODO validate values?

        // check for authorization
        String authorizationType = kvps.get(ConnectionStringKeys.AUTHORIZATION);
        if (authorizationType != null && !"ikey".equalsIgnoreCase(authorizationType)) {
            throw new UnsupportedAuthorizationTypeException(authorizationType + " is not a supported Authorization value. Supported values: [\"ikey\"].");
        }

        // get ikey
        String instrumentationKey = kvps.remove(ConnectionStringKeys.INSTRUMENTATION_KEY);
        if (Strings.isNullOrEmpty(instrumentationKey)) {
            throw new InvalidConnectionStringException("Missing 'InstrumentationKey'");
        }
        result.setInstrumentationKey(instrumentationKey);

        // resolve suffix
        String suffix = kvps.get(ConnectionStringKeys.ENDPOINT_SUFFIX);
        if (!Strings.isNullOrEmpty(suffix)) {
            // resolve location
            String location = kvps.get(ConnectionStringKeys.LOCATION);
            result.setIngestionEndpoint(constructSecureEndpoint(location, EndpointPrefixes.INGESTION_ENDPOINT_PREFIX, suffix));
            result.setLiveEndpoint(constructSecureEndpoint(location, EndpointPrefixes.LIVE_ENDPOINT_PREFIX, suffix));
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

        return result;
    }

    @VisibleForTesting
    static String constructSecureEndpoint(String location, String prefix, String suffix) {
        return Strings.isNullOrEmpty(location)
                ? constructSecureEndpoint(prefix, suffix)
                : "https://" + location + "." + prefix + "." + suffix;
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
        static final String LOCATION = "Location";
    }

    @VisibleForTesting
    static class EndpointPrefixes {
        private EndpointPrefixes(){}
        static final String INGESTION_ENDPOINT_PREFIX = "dc";
        static final String LIVE_ENDPOINT_PREFIX = "live";
        static final String DEFAULT_LIVE_ENDPOINT_PREFIX = "rt";
    }

    public static class Defaults {
        @VisibleForTesting
        static final String ENDPOINT_SUFFIX = "services.visualstudio.com";

        public static final String INGESTION_ENDPOINT = constructSecureEndpoint(EndpointPrefixes.INGESTION_ENDPOINT_PREFIX, ENDPOINT_SUFFIX);
        public static final String LIVE_ENDPOINT = constructSecureEndpoint(EndpointPrefixes.DEFAULT_LIVE_ENDPOINT_PREFIX, ENDPOINT_SUFFIX);
    }
}
