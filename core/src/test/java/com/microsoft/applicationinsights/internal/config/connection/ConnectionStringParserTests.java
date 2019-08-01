package com.microsoft.applicationinsights.internal.config.connection;

import com.microsoft.applicationinsights.internal.config.ConnectionConfiguration;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.EndpointPrefixes;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static com.microsoft.applicationinsights.internal.config.connection.ConnectionString.parse;
import static org.junit.Assert.*;

public class ConnectionStringParserTests {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void minimalString() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey;

        ConnectionConfiguration config = parse(cs);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(ConnectionString.Defaults.INGESTION_ENDPOINT, config.getIngestionEndpoint());
        assertEquals(ConnectionString.Defaults.LIVE_ENDPOINT, config.getLiveEndpoint());
    }

    @Test
    public void minimalWithAuth() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String cs = "Authorization=ikey;InstrumentationKey="+ikey;

        ConnectionConfiguration config = parse(cs);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(ConnectionString.Defaults.INGESTION_ENDPOINT, config.getIngestionEndpoint());
        assertEquals(ConnectionString.Defaults.LIVE_ENDPOINT, config.getLiveEndpoint());
    }

    @Test
    public void ikeyWithSuffix() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix="+suffix;
        final String expectedIngestionEndpoint = "https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix;
        final String expectedLiveEndpoint = "https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix;

        ConnectionConfiguration config = parse(cs);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getLiveEndpoint());
    }

    @Test
    public void ikeyWithExplicitEndpoints() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String expectedIngestionEndpoint = "https://ingestion.example.com";
        final String expectedLiveEndpoint = "https://live.example.com";
        final String cs = "InstrumentationKey="+ikey+";IngestionEndpoint="+expectedIngestionEndpoint+";LiveEndpoint="+expectedLiveEndpoint;

        ConnectionConfiguration config = parse(cs);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getLiveEndpoint());
    }

    @Test
    public void explicitEndpointOverridesSuffix() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final String expectedIngestionEndpoint = "https://ingestion.example.com";
        final String expectedLiveEndpoint = "https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix;
        final String cs = "InstrumentationKey="+ikey+";IngestionEndpoint="+expectedIngestionEndpoint+";EndpointSuffix="+suffix;

        ConnectionConfiguration config = parse(cs);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getLiveEndpoint());
    }

    @Test
    public void locationWithoutSuffixPrefixesDefaultValues() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String location = "westus2";
        final String cs = "InstrumentationKey="+ikey+";Location="+location;

        final String expectedIngestionEndpoint = ConnectionString.constructSecureEndpoint(location, EndpointPrefixes.INGESTION_ENDPOINT_PREFIX, Defaults.ENDPOINT_SUFFIX);
        final String expectedLiveEndpoint = ConnectionString.constructSecureEndpoint(location, EndpointPrefixes.DEFAULT_LIVE_ENDPOINT_PREFIX, Defaults.ENDPOINT_SUFFIX);

        ConnectionConfiguration config = parse(cs);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getLiveEndpoint());
    }

    @Test
    public void emptyPairIsIgnored() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final String cs = "InstrumentationKey="+ikey+";;EndpointSuffix="+suffix+";";
        final String expectedIngestionEndpoint = "https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix;
        final String expectedLiveEndpoint = "https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix;
        ConnectionConfiguration config;
        try {
            config = parse(cs);
        } catch (Exception e) {
            fail("Exception thrown from parse: " + ExceptionUtils.getStackTrace(e));
            return;
        }
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getLiveEndpoint());
    }

    @Test
    public void emptyKeyIsIgnored() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey+";=1234";
        final String expectedIngestionEndpoint = Defaults.INGESTION_ENDPOINT;
        final String expectedLiveEndpoint = Defaults.LIVE_ENDPOINT;
        ConnectionConfiguration config;
        try {
            config = parse(cs);
        } catch (Exception e) {
            fail("Exception thrown from parse: " + ExceptionUtils.getStackTrace(e));
            return;
        }
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getLiveEndpoint());
    }

    @Test
    public void emptyValueIsSameAsUnset() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix=";

        ConnectionConfiguration config = parse(cs);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(Defaults.INGESTION_ENDPOINT, config.getIngestionEndpoint());
        assertEquals(Defaults.LIVE_ENDPOINT, config.getLiveEndpoint());
    }

    @Test
    public void emptyIkeyValueIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        final String cs = "InstrumentationKey=;IngestionEndpoint=https://ingestion.example.com;EndpointSuffix=ai.example.com";
        ConnectionConfiguration config = parse(cs);
    }

    @Test
    public void multipleKeySeparatorsIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        final String ikey = "fake-ikey";
        final String cs = "Authorization=ikey;InstrumentationKey=="+ikey;
        ConnectionConfiguration config = parse(cs);
    }

    @Test
    public void emptyStringIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        ConnectionConfiguration config = parse("");
    }

    @Test
    public void nonKeyValueStringIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        ConnectionConfiguration config = parse(UUID.randomUUID().toString());
    }

    @Test
    public void missingAuthorizationIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        ConnectionConfiguration config = parse("LiveEndpoint=https://live.example.com");
    }

    @Test
    public void nonIkeyAuthIsInvalid() throws ConnectionStringParseException {
        exception.expect(UnsupportedAuthorizationTypeException.class);
        ConnectionConfiguration config = parse("Authorization=magic;MagicWord=abacadabra");
    }

}
