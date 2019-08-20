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

        ConnectionString.parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT), config.getEndpointProvider().getIngestionEndpoint());
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URI_PATH), config.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(URI.create(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URI_PATH), config.getEndpointProvider().getLiveEndpointURL());
    }

    @Test // this test does not use this.config
    public void appIdUrlIsConstructedWithIkeyFromIngestionEndpoint() {
        EndpointProvider ep = new EndpointProvider();
        String ikey = "fake-ikey";
        final String host = "http://123.com";
        ep.setIngestionEndpoint(URI.create(host));
        assertEquals(URI.create(host+"/"+EndpointProvider.API_PROFILES_APP_ID_URI_PREFIX+ikey+EndpointProvider.API_PROFILES_APP_ID_URI_SUFFIX), ep.getAppIdEndpointURL(ikey));
    }

    @Test
    public void minimalWithAuth() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String cs = "Authorization=ikey;InstrumentationKey="+ikey;

        ConnectionString.parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT), config.getEndpointProvider().getIngestionEndpoint());
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URI_PATH), config.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(URI.create(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URI_PATH), config.getEndpointProvider().getLiveEndpointURL());
    }

    @Test
    public void ikeyWithSuffix() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix="+suffix;
        final URI expectedIngestionEndpoint = URI.create("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix);
        final URI expectedIngestionEndpointURL = URI.create("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix + "/" + EndpointProvider.INGESTION_URI_PATH);
        final URI expectedLiveEndpoint = URI.create("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix + "/" + EndpointProvider.LIVE_URI_PATH);

        ConnectionString.parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, config.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(expectedLiveEndpoint, config.getEndpointProvider().getLiveEndpointURL());
    }

    @Test
    public void ikeyWithExplicitEndpoints() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final URI expectedIngestionEndpoint = URI.create("https://ingestion.example.com");
        final URI expectedIngestionEndpointURL = URI.create("https://ingestion.example.com/" + EndpointProvider.INGESTION_URI_PATH);
        final String liveHost = "https://live.example.com";
        final URI expectedLiveEndpoint = URI.create(liveHost + "/" + EndpointProvider.LIVE_URI_PATH);
        final String cs = "InstrumentationKey="+ikey+";IngestionEndpoint="+expectedIngestionEndpoint+";LiveEndpoint="+liveHost;

        ConnectionString.parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, config.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(expectedLiveEndpoint, config.getEndpointProvider().getLiveEndpointURL());
    }

    @Test
    public void explicitEndpointOverridesSuffix() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final URI expectedIngestionEndpoint = URI.create("https://ingestion.example.com");
        final URI expectedIngestionEndpointURL = URI.create("https://ingestion.example.com/" + EndpointProvider.INGESTION_URI_PATH);
        final URI expectedLiveEndpoint = URI.create("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix+"/"+EndpointProvider.LIVE_URI_PATH);
        final String cs = "InstrumentationKey="+ikey+";IngestionEndpoint="+expectedIngestionEndpoint+";EndpointSuffix="+suffix;

        ConnectionString.parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, config.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(expectedLiveEndpoint, config.getEndpointProvider().getLiveEndpointURL());
    }

    @Test
    public void emptyPairIsIgnored() {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final String cs = "InstrumentationKey="+ikey+";;EndpointSuffix="+suffix+";";
        final URI expectedIngestionEndpoint = URI.create("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix);
        final URI expectedIngestionEndpointURL = URI.create("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix+"/" + EndpointProvider.INGESTION_URI_PATH);
        final URI expectedLiveEndpoint = URI.create("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix + "/" + EndpointProvider.LIVE_URI_PATH);
        try {
            ConnectionString.parseInto(cs, config);
        } catch (Exception e) {
            fail("Exception thrown from parse: " + ExceptionUtils.getStackTrace(e));
            return;
        }
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, config.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(expectedLiveEndpoint, config.getEndpointProvider().getLiveEndpointURL());
    }

    @Test
    public void emptyKeyIsIgnored() {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey+";=1234";
        final URI expectedIngestionEndpoint = URI.create(Defaults.INGESTION_ENDPOINT);
        final URI expectedIngestionEndpointURL = URI.create(Defaults.INGESTION_ENDPOINT+"/"+EndpointProvider.INGESTION_URI_PATH);
        final URI expectedLiveEndpoint = URI.create(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URI_PATH);
        try {
            ConnectionString.parseInto(cs, config);
        } catch (Exception e) {
            fail("Exception thrown from parse: " + ExceptionUtils.getStackTrace(e));
            return;
        }
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, config.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(expectedLiveEndpoint, config.getEndpointProvider().getLiveEndpointURL());
    }

    @Test
    public void emptyValueIsSameAsUnset() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix=";

        ConnectionString.parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT), config.getEndpointProvider().getIngestionEndpoint());
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URI_PATH), config.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(URI.create(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URI_PATH), config.getEndpointProvider().getLiveEndpointURL());
    }

    @Test
    public void caseInsensitiveParsing() throws ConnectionStringParseException {
        final String ikey = "fake-ikey";
        final String live = "https://live.something.com";
        final String profiler = "https://prof.something.com";
        final String cs1 = "InstrumentationKey="+ ikey +";LiveEndpoint="+ live +";ProfilerEndpoint="+ profiler;
        final String cs2 = "instRUMentationkEY="+ ikey +";LivEEndPOINT="+ live +";ProFILErEndPOinT="+ profiler;

        TelemetryConfiguration config2 = new TelemetryConfiguration();

        ConnectionString.parseInto(cs1, config);
        ConnectionString.parseInto(cs2, config2);

        assertEquals(config.getInstrumentationKey(), config2.getInstrumentationKey());
        assertEquals(config.getEndpointProvider().getIngestionEndpoint(), config2.getEndpointProvider().getIngestionEndpoint());
        assertEquals(config.getEndpointProvider().getIngestionEndpointURL(), config2.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(config.getEndpointProvider().getLiveEndpointURL(), config2.getEndpointProvider().getLiveEndpointURL());
        assertEquals(config.getEndpointProvider().getProfilerEndpoint(), config2.getEndpointProvider().getProfilerEndpoint());
        assertEquals(config.getEndpointProvider().getSnapshotEndpoint(), config2.getEndpointProvider().getSnapshotEndpoint());
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

        ConnectionString.parseInto(cs1, config);
        ConnectionString.parseInto(cs2, config2);

        assertEquals(config.getInstrumentationKey(), config2.getInstrumentationKey());
        assertEquals(config.getEndpointProvider().getIngestionEndpoint(), config2.getEndpointProvider().getIngestionEndpoint());
        assertEquals(config.getEndpointProvider().getIngestionEndpointURL(), config2.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(config.getEndpointProvider().getLiveEndpointURL(), config2.getEndpointProvider().getLiveEndpointURL());
        assertEquals(config.getEndpointProvider().getProfilerEndpoint(), config2.getEndpointProvider().getProfilerEndpoint());
        assertEquals(config.getEndpointProvider().getSnapshotEndpoint(), config2.getEndpointProvider().getSnapshotEndpoint());
    }

    @Test
    public void endpointWithNoSchemeIsHttps() throws ConnectionStringParseException {
        ConnectionString.parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com", config);
        assertEquals("https", config.getEndpointProvider().getIngestionEndpoint().getScheme());
    }

    @Test
    public void httpEndpointKeepsScheme() throws ConnectionStringParseException {
        ConnectionString.parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=http://my-ai.example.com", config);
        assertEquals("http", config.getEndpointProvider().getIngestionEndpoint().getScheme());
    }

    @Test
    public void emptyIkeyValueIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        final String cs = "InstrumentationKey=;IngestionEndpoint=https://ingestion.example.com;EndpointSuffix=ai.example.com";
        ConnectionString.parseInto(cs, config);
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
        ConnectionString.parseInto("", config);
    }

    @Test
    public void nonKeyValueStringIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        ConnectionString.parseInto(UUID.randomUUID().toString(), config);
    }

    @Test
    public void missingAuthorizationIsInvalid() throws ConnectionStringParseException {
        exception.expect(InvalidConnectionStringException.class);
        ConnectionString.parseInto("LiveEndpoint=https://live.example.com", config);
    }

    @Test
    public void nonIkeyAuthIsInvalid() throws ConnectionStringParseException {
        exception.expect(UnsupportedAuthorizationTypeException.class);
        ConnectionString.parseInto("Authorization=magic;MagicWord=abacadabra", config);
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
            ConnectionString.parseInto(connectionString, config);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
