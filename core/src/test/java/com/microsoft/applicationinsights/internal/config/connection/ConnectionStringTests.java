package com.microsoft.applicationinsights.internal.config.connection;

import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.EndpointPrefixes;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static com.microsoft.applicationinsights.internal.config.connection.ConnectionString.parseInto;
import static org.junit.Assert.*;

public class ConnectionStringTests {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ConnectionConfiguration config = null;

    @Before
    public void setup() {
        config = new ConnectionConfiguration();
    }

    @After
    public void teardown() {
        config = null;
    }

    @Test
    public void minimalString() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey;

        parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(ConnectionString.Defaults.INGESTION_ENDPOINT, config.getIngestionEndpoint());
        assertEquals(ConnectionString.Defaults.LIVE_ENDPOINT, config.getLiveEndpoint());
    }

    @Test
    public void minimalWithAuth() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String cs = "Authorization=ikey;InstrumentationKey="+ikey;

        parseInto(cs, config);
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

        parseInto(cs, config);
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

        parseInto(cs, config);
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

        parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getLiveEndpoint());
    }

    @Test
    public void emptyPairIsIgnored() {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final String cs = "InstrumentationKey="+ikey+";;EndpointSuffix="+suffix+";";
        final String expectedIngestionEndpoint = "https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix;
        final String expectedLiveEndpoint = "https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix;
        try {
            parseInto(cs, config);
        } catch (Exception e) {
            fail("Exception thrown from parse: " + ExceptionUtils.getStackTrace(e));
            return;
        }
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getLiveEndpoint());
    }

    @Test
    public void emptyKeyIsIgnored() {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey+";=1234";
        final String expectedIngestionEndpoint = Defaults.INGESTION_ENDPOINT;
        final String expectedLiveEndpoint = Defaults.LIVE_ENDPOINT;
        try {
            parseInto(cs, config);
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

        parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(Defaults.INGESTION_ENDPOINT, config.getIngestionEndpoint());
        assertEquals(Defaults.LIVE_ENDPOINT, config.getLiveEndpoint());
    }

    @Test
    public void caseInsensitiveParsing() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String live = "https://live.something.com";
        final String profiler = "https://prof.something.com";
        final String cs1 = "InstrumentationKey="+ ikey +";LiveEndpoint="+ live +";ProfilerEndpoint="+ profiler;
        final String cs2 = "instRUMentationkEY="+ ikey +";LivEEndPOINT="+ live +";ProFILErEndPOinT="+ profiler;

        ConnectionConfiguration config2 = new ConnectionConfiguration();

        parseInto(cs1, config);
        parseInto(cs2, config2);

        assertEquals(config.getInstrumentationKey(), config2.getInstrumentationKey());
        assertEquals(config.getIngestionEndpoint(), config2.getIngestionEndpoint());
        assertEquals(config.getLiveEndpoint(), config2.getLiveEndpoint());
        assertEquals(config.getProfilerEndpoint(), config2.getProfilerEndpoint());
        assertEquals(config.getSnapshotEndpoint(), config2.getSnapshotEndpoint());
    }

    @Test
    public void orderDoesNotMatter() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String live = "https://live.something.com";
        final String profiler = "https://prof.something.com";
        final String snapshot = "https://whatever.snappy.com";
        final String cs1 = "InstrumentationKey="+ ikey +";LiveEndpoint="+ live +";ProfilerEndpoint="+ profiler+";SnapshotEndpoint="+ snapshot;
        final String cs2 = "SnapshotEndpoint="+ snapshot+";ProfilerEndpoint="+ profiler+";InstrumentationKey="+ ikey +";LiveEndpoint="+ live;

        ConnectionConfiguration config2 = new ConnectionConfiguration();

        parseInto(cs1, config);
        parseInto(cs2, config2);

        assertEquals(config.getInstrumentationKey(), config2.getInstrumentationKey());
        assertEquals(config.getIngestionEndpoint(), config2.getIngestionEndpoint());
        assertEquals(config.getLiveEndpoint(), config2.getLiveEndpoint());
        assertEquals(config.getProfilerEndpoint(), config2.getProfilerEndpoint());
        assertEquals(config.getSnapshotEndpoint(), config2.getSnapshotEndpoint());
    }

    @Test
    public void emptyIkeyValueIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        final String cs = "InstrumentationKey=;IngestionEndpoint=https://ingestion.example.com;EndpointSuffix=ai.example.com";
        parseInto(cs, config);
    }

    @Test
    public void multipleKeySeparatorsIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        final String ikey = "fake-ikey";
        final String cs = "Authorization=ikey;InstrumentationKey=="+ikey;
        parseInto(cs, config);
    }

    @Test
    public void emptyStringIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        parseInto("", config);
    }

    @Test
    public void nonKeyValueStringIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        parseInto(UUID.randomUUID().toString(), config);
    }

    @Test
    public void missingAuthorizationIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        parseInto("LiveEndpoint=https://live.example.com", config);
    }

    @Test
    public void nonIkeyAuthIsInvalid() throws ConnectionStringParseException {
        exception.expect(UnsupportedAuthorizationTypeException.class);
        parseInto("Authorization=magic;MagicWord=abacadabra", config);
    }

}
