package com.microsoft.applicationinsights.internal.config.connection;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.EndpointPrefixes;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
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
    public void minimalString() throws Exception {
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
    public void appIdUrlWithPathKeepsIt() {
        EndpointProvider ep = new EndpointProvider();
        String ikey = "fake-ikey";
        String url = "http://123.com/path/321";
        ep.setIngestionEndpoint(URI.create(url));
        assertEquals(URI.create(url+"/"+EndpointProvider.API_PROFILES_APP_ID_URI_PREFIX+ikey+EndpointProvider.API_PROFILES_APP_ID_URI_SUFFIX), ep.getAppIdEndpointURL(ikey));

        ep.setIngestionEndpoint(URI.create(url+"/"));
        assertEquals(URI.create(url+"/"+EndpointProvider.API_PROFILES_APP_ID_URI_PREFIX+ikey+EndpointProvider.API_PROFILES_APP_ID_URI_SUFFIX), ep.getAppIdEndpointURL(ikey));
    }

    @Test
    public void ikeyWithSuffix() throws Exception {
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
    public void suffixWithPathRetainsThePath() throws Exception {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com/my-proxy-app/doProxy";
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
    public void suffixSupportsPort() throws Exception {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com:9999";
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
    public void ikeyWithExplicitEndpoints() throws Exception {
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
    public void explicitEndpointOverridesSuffix() throws Exception {
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
            throw new AssertionError("Exception thrown from parse");
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
            throw new AssertionError("Exception thrown from parse");
        }
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, config.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, config.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(expectedLiveEndpoint, config.getEndpointProvider().getLiveEndpointURL());
    }

    @Test
    public void emptyValueIsSameAsUnset() throws Exception {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix=";

        ConnectionString.parseInto(cs, config);
        assertEquals(ikey, config.getInstrumentationKey());
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT), config.getEndpointProvider().getIngestionEndpoint());
        assertEquals(URI.create(Defaults.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URI_PATH), config.getEndpointProvider().getIngestionEndpointURL());
        assertEquals(URI.create(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URI_PATH), config.getEndpointProvider().getLiveEndpointURL());
    }

    @Test
    public void caseInsensitiveParsing() throws Exception {
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
    public void orderDoesNotMatter() throws Exception {
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
    public void endpointWithNoSchemeIsInvalid() throws Exception {
        exception.expect(InvalidConnectionStringException.class);
        exception.expectMessage(containsString("IngestionEndpoint"));
        ConnectionString.parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com", config);
    }

    @Test
    public void endpointWithPathMissingSchemeIsInvalid() throws Exception {
        exception.expect(InvalidConnectionStringException.class);
        exception.expectMessage(containsString("IngestionEndpoint"));
        ConnectionString.parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com/path/prefix", config);
    }

    @Test
    public void endpointWithPortMissingSchemeIsInvalid() throws Exception {
        exception.expect(InvalidConnectionStringException.class);
        exception.expectMessage(containsString("IngestionEndpoint"));
        ConnectionString.parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com:9999", config);
    }

    @Test
    public void httpEndpointKeepsScheme() throws Exception {
        ConnectionString.parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=http://my-ai.example.com", config);
        assertEquals(URI.create("http://my-ai.example.com"), config.getEndpointProvider().getIngestionEndpoint());
    }

    @Test
    public void emptyIkeyValueIsInvalid() throws Exception {
        exception.expect(InvalidConnectionStringException.class);
        final String cs = "InstrumentationKey=;IngestionEndpoint=https://ingestion.example.com;EndpointSuffix=ai.example.com";
        ConnectionString.parseInto(cs, config);
    }

    @Test
    public void multipleKeySeparatorsIsInvalid() throws Exception {
        exception.expect(InvalidConnectionStringException.class);
        final String ikey = "fake-ikey";
        exception.expectMessage(not(containsString(ikey))); // ikey is a secret; should not be in log/exception message
        final String cs = "InstrumentationKey=="+ikey;
        parseInto_printExceptionAndRethrow(cs);
    }

    @Test
    public void emptyStringIsInvalid() throws Exception {
        exception.expect(InvalidConnectionStringException.class);
        ConnectionString.parseInto("", config);
    }

    @Test
    public void nonKeyValueStringIsInvalid() throws Exception {
        exception.expect(InvalidConnectionStringException.class);
        ConnectionString.parseInto(UUID.randomUUID().toString(), config);
    }

    @Test // when more Authorization values are available, create a copy of this test. For example, given "Authorization=Xyz", this would fail because the 'Xyz' key/value pair is missing.
    public void missingInstrumentationKeyIsInvalid() throws Exception {
        exception.expect(InvalidConnectionStringException.class);
        ConnectionString.parseInto("LiveEndpoint=https://live.example.com", config);
    }

    @Test
    public void invalidUriIsInvalidConnectionString() throws Exception {
        exception.expect(InvalidConnectionStringException.class);
        exception.expectCause(Matchers.<Throwable>instanceOf(URISyntaxException.class));
        exception.expectMessage(containsString("LiveEndpoint"));
        parseInto_printExceptionAndRethrow("InstrumentationKey=fake-ikey;LiveEndpoint=https:////~!@#$%&^*()_{}{}><?<?>:L\":\"_+_+_");
    }

    @Test
    public void giantValuesAreNotAllowed() throws Exception {
        exception.expect(InvalidConnectionStringException.class);
        exception.expectMessage(containsString(""+ConnectionString.CONNECTION_STRING_MAX_LENGTH)); // message should state max length
        String bigIkey = StringUtils.repeat('0', ConnectionString.CONNECTION_STRING_MAX_LENGTH * 2);
        parseInto_printExceptionAndRethrow("InstrumentationKey="+bigIkey);
    }

    private void parseInto_printExceptionAndRethrow(String connectionString) throws Exception {
        try {
            ConnectionString.parseInto(connectionString, config);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
