package com.microsoft.applicationinsights.internal.config.connection;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.EndpointPrefixes;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static com.microsoft.applicationinsights.internal.config.connection.ConnectionString.parseInto;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

public class ConnectionStringParsingTests {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private TelemetryConfiguration config = null;

    @Before
    public void setup() {
        config = new TelemetryConfiguration();
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
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT), config.getEndpointConfiguration().getIngestionEndpoint());
        assertEquals(URI.create(Defaults.LIVE_ENDPOINT), config.getEndpointConfiguration().getLiveEndpoint());
    }

    @Test
    public void minimalWithAuth() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String cs = "Authorization=ikey;InstrumentationKey="+ikey;

        parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT), config.getEndpointConfiguration().getIngestionEndpoint());
        assertEquals(URI.create(Defaults.LIVE_ENDPOINT), config.getEndpointConfiguration().getLiveEndpoint());
    }

    @Test
    public void ikeyWithSuffix() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix="+suffix;
        final URI expectedIngestionEndpoint = URI.create("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix);
        final URI expectedLiveEndpoint = URI.create("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix);

        parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointConfiguration().getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getEndpointConfiguration().getLiveEndpoint());
    }

    @Test
    public void ikeyWithExplicitEndpoints() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final URI expectedIngestionEndpoint = URI.create("https://ingestion.example.com");
        final URI expectedLiveEndpoint = URI.create("https://live.example.com");
        final String cs = "InstrumentationKey="+ikey+";IngestionEndpoint="+expectedIngestionEndpoint+";LiveEndpoint="+expectedLiveEndpoint;

        parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointConfiguration().getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getEndpointConfiguration().getLiveEndpoint());
    }

    @Test
    public void explicitEndpointOverridesSuffix() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final URI expectedIngestionEndpoint = URI.create("https://ingestion.example.com");
        final URI expectedLiveEndpoint = URI.create("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix);
        final String cs = "InstrumentationKey="+ikey+";IngestionEndpoint="+expectedIngestionEndpoint+";EndpointSuffix="+suffix;

        parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointConfiguration().getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getEndpointConfiguration().getLiveEndpoint());
    }

    @Test
    public void emptyPairIsIgnored() {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final String cs = "InstrumentationKey="+ikey+";;EndpointSuffix="+suffix+";";
        final URI expectedIngestionEndpoint = URI.create("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix);
        final URI expectedLiveEndpoint = URI.create("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix);
        try {
            parseInto(cs, config);
        } catch (Exception e) {
            fail("Exception thrown from parse: " + ExceptionUtils.getStackTrace(e));
            return;
        }
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointConfiguration().getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getEndpointConfiguration().getLiveEndpoint());
    }

    @Test
    public void emptyKeyIsIgnored() {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey+";=1234";
        final URI expectedIngestionEndpoint = URI.create(Defaults.INGESTION_ENDPOINT);
        final URI expectedLiveEndpoint = URI.create(Defaults.LIVE_ENDPOINT);
        try {
            parseInto(cs, config);
        } catch (Exception e) {
            fail("Exception thrown from parse: " + ExceptionUtils.getStackTrace(e));
            return;
        }
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointConfiguration().getIngestionEndpoint());
        assertEquals(expectedLiveEndpoint, config.getEndpointConfiguration().getLiveEndpoint());
    }

    @Test
    public void emptyValueIsSameAsUnset() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix=";

        parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT), config.getEndpointConfiguration().getIngestionEndpoint());
        assertEquals(URI.create(Defaults.LIVE_ENDPOINT), config.getEndpointConfiguration().getLiveEndpoint());
    }

    @Test
    public void caseInsensitiveParsing() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String live = "https://live.something.com";
        final String profiler = "https://prof.something.com";
        final String cs1 = "InstrumentationKey="+ ikey +";LiveEndpoint="+ live +";ProfilerEndpoint="+ profiler;
        final String cs2 = "instRUMentationkEY="+ ikey +";LivEEndPOINT="+ live +";ProFILErEndPOinT="+ profiler;

        TelemetryConfiguration config2 = new TelemetryConfiguration();

        parseInto(cs1, config);
        parseInto(cs2, config2);

        assertEquals(config.getInstrumentationKey(), config2.getInstrumentationKey());
        assertEquals(config.getEndpointConfiguration().getIngestionEndpoint(), config2.getEndpointConfiguration().getIngestionEndpoint());
        assertEquals(config.getEndpointConfiguration().getLiveEndpoint(), config2.getEndpointConfiguration().getLiveEndpoint());
        assertEquals(config.getEndpointConfiguration().getProfilerEndpoint(), config2.getEndpointConfiguration().getProfilerEndpoint());
        assertEquals(config.getEndpointConfiguration().getSnapshotEndpoint(), config2.getEndpointConfiguration().getSnapshotEndpoint());
    }

    @Test
    public void orderDoesNotMatter() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String live = "https://live.something.com";
        final String profiler = "https://prof.something.com";
        final String snapshot = "https://whatever.snappy.com";
        final String cs1 = "InstrumentationKey="+ ikey +";LiveEndpoint="+ live +";ProfilerEndpoint="+ profiler+";SnapshotEndpoint="+ snapshot;
        final String cs2 = "SnapshotEndpoint="+ snapshot+";ProfilerEndpoint="+ profiler+";InstrumentationKey="+ ikey +";LiveEndpoint="+ live;

        TelemetryConfiguration config2 = new TelemetryConfiguration();

        parseInto(cs1, config);
        parseInto(cs2, config2);

        assertEquals(config.getInstrumentationKey(), config2.getInstrumentationKey());
        assertEquals(config.getEndpointConfiguration().getIngestionEndpoint(), config2.getEndpointConfiguration().getIngestionEndpoint());
        assertEquals(config.getEndpointConfiguration().getLiveEndpoint(), config2.getEndpointConfiguration().getLiveEndpoint());
        assertEquals(config.getEndpointConfiguration().getProfilerEndpoint(), config2.getEndpointConfiguration().getProfilerEndpoint());
        assertEquals(config.getEndpointConfiguration().getSnapshotEndpoint(), config2.getEndpointConfiguration().getSnapshotEndpoint());
    }

    @Test
    public void endpointWithNoSchemeIsHttps() throws ConnectionStringParseException {
        parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com", config);
        assertEquals("https", config.getEndpointConfiguration().getIngestionEndpoint().getScheme());
    }

    @Test
    public void httpEndpointKeepsScheme() throws ConnectionStringParseException {
        parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=http://my-ai.example.com", config);
        assertEquals("http", config.getEndpointConfiguration().getIngestionEndpoint().getScheme());
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
        parseInto_printExceptionAndRethrow(cs);
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

    @Test
    public void invalidUriIsInvalidConnectionString() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        exception.expectCause(Matchers.<Throwable>instanceOf(URISyntaxException.class));
        exception.expectMessage(containsString("LiveEndpoint"));
        parseInto_printExceptionAndRethrow("InstrumentationKey=fake-ikey;LiveEndpoint=https:////~!@#$%&^*()_{}{}><?<?>:L\":\"_+_+_");
    }

    private void parseInto_printExceptionAndRethrow(String connectionString) throws ConnectionStringParseException {
        try {
            parseInto(connectionString, config);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
