package com.microsoft.applicationinsights.internal.config.connection;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.EndpointPrefixes;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConnectionStringParsingTests {

    private final TelemetryClient telemetryClient = new TelemetryClient();

    @Test
    public void minimalString() throws Exception {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey;

        ConnectionString.parseInto(cs, telemetryClient);
        assertEquals(ikey, telemetryClient.getInstrumentationKey());
        assertEquals(new URL(Defaults.INGESTION_ENDPOINT), telemetryClient.getEndpointProvider().getIngestionEndpoint());
        assertEquals(new URL(Defaults.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URL_PATH), telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(new URL(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URL_PATH), telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    }

    @Test // this test does not use this.config
    public void appIdUrlIsConstructedWithIkeyFromIngestionEndpoint() throws MalformedURLException {
        EndpointProvider ep = new EndpointProvider();
        String ikey = "fake-ikey";
        final String host = "http://123.com";
        ep.setIngestionEndpoint(new URL(host));
        assertEquals(new URL(host+"/"+EndpointProvider.API_PROFILES_APP_ID_URL_PREFIX +ikey+EndpointProvider.API_PROFILES_APP_ID_URL_SUFFIX), ep.getAppIdEndpointUrl(ikey));
    }

    @Test
    public void appIdUrlWithPathKeepsIt() throws MalformedURLException {
        EndpointProvider ep = new EndpointProvider();
        String ikey = "fake-ikey";
        String url = "http://123.com/path/321";
        ep.setIngestionEndpoint(new URL(url));
        assertEquals(new URL(url+"/"+EndpointProvider.API_PROFILES_APP_ID_URL_PREFIX +ikey+EndpointProvider.API_PROFILES_APP_ID_URL_SUFFIX), ep.getAppIdEndpointUrl(ikey));

        ep.setIngestionEndpoint(new URL(url+"/"));
        assertEquals(new URL(url+"/"+EndpointProvider.API_PROFILES_APP_ID_URL_PREFIX +ikey+EndpointProvider.API_PROFILES_APP_ID_URL_SUFFIX), ep.getAppIdEndpointUrl(ikey));
    }

    @Test
    public void ikeyWithSuffix() throws Exception {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix="+suffix;
        final URL expectedIngestionEndpoint = new URL("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix);
        final URL expectedIngestionEndpointURL = new URL("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix + "/" + EndpointProvider.INGESTION_URL_PATH);
        final URL expectedLiveEndpoint = new URL("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix + "/" + EndpointProvider.LIVE_URL_PATH);

        ConnectionString.parseInto(cs, telemetryClient);
        assertEquals(ikey, telemetryClient.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, telemetryClient.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(expectedLiveEndpoint, telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    }

    @Test
    public void suffixWithPathRetainsThePath() throws Exception {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com/my-proxy-app/doProxy";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix="+suffix;
        final URL expectedIngestionEndpoint = new URL("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix);
        final URL expectedIngestionEndpointURL = new URL("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix + "/" + EndpointProvider.INGESTION_URL_PATH);
        final URL expectedLiveEndpoint = new URL("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix + "/" + EndpointProvider.LIVE_URL_PATH);

        ConnectionString.parseInto(cs, telemetryClient);
        assertEquals(ikey, telemetryClient.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, telemetryClient.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(expectedLiveEndpoint, telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    }

    @Test
    public void suffixSupportsPort() throws Exception {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com:9999";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix="+suffix;
        final URL expectedIngestionEndpoint = new URL("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix);
        final URL expectedIngestionEndpointURL = new URL("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix + "/" + EndpointProvider.INGESTION_URL_PATH);
        final URL expectedLiveEndpoint = new URL("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix + "/" + EndpointProvider.LIVE_URL_PATH);

        ConnectionString.parseInto(cs, telemetryClient);
        assertEquals(ikey, telemetryClient.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, telemetryClient.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(expectedLiveEndpoint, telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    }

    @Test
    public void ikeyWithExplicitEndpoints() throws Exception {
        final String ikey = "fake-ikey";
        final URL expectedIngestionEndpoint = new URL("https://ingestion.example.com");
        final URL expectedIngestionEndpointURL = new URL("https://ingestion.example.com/" + EndpointProvider.INGESTION_URL_PATH);
        final String liveHost = "https://live.example.com";
        final URL expectedLiveEndpoint = new URL(liveHost + "/" + EndpointProvider.LIVE_URL_PATH);
        final String cs = "InstrumentationKey="+ikey+";IngestionEndpoint="+expectedIngestionEndpoint+";LiveEndpoint="+liveHost;

        ConnectionString.parseInto(cs, telemetryClient);
        assertEquals(ikey, telemetryClient.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, telemetryClient.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(expectedLiveEndpoint, telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    }

    @Test
    public void explicitEndpointOverridesSuffix() throws Exception {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final URL expectedIngestionEndpoint = new URL("https://ingestion.example.com");
        final URL expectedIngestionEndpointURL = new URL("https://ingestion.example.com/" + EndpointProvider.INGESTION_URL_PATH);
        final URL expectedLiveEndpoint = new URL("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix+"/"+EndpointProvider.LIVE_URL_PATH);
        final String cs = "InstrumentationKey="+ikey+";IngestionEndpoint="+expectedIngestionEndpoint+";EndpointSuffix="+suffix;

        ConnectionString.parseInto(cs, telemetryClient);
        assertEquals(ikey, telemetryClient.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, telemetryClient.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(expectedLiveEndpoint, telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    }

    @Test
    public void emptyPairIsIgnored() throws MalformedURLException, InvalidConnectionStringException {
        final String ikey = "fake-ikey";
        final String suffix = "ai.example.com";
        final String cs = "InstrumentationKey="+ikey+";;EndpointSuffix="+suffix+";";
        final URL expectedIngestionEndpoint = new URL("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix);
        final URL expectedIngestionEndpointURL = new URL("https://"+EndpointPrefixes.INGESTION_ENDPOINT_PREFIX+"."+suffix+"/" + EndpointProvider.INGESTION_URL_PATH);
        final URL expectedLiveEndpoint = new URL("https://"+EndpointPrefixes.LIVE_ENDPOINT_PREFIX+"."+suffix + "/" + EndpointProvider.LIVE_URL_PATH);
        ConnectionString.parseInto(cs, telemetryClient);
        assertEquals(ikey, telemetryClient.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, telemetryClient.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(expectedLiveEndpoint, telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    }

    @Test
    public void emptyKeyIsIgnored() throws MalformedURLException {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey+";=1234";
        final URL expectedIngestionEndpoint = new URL(Defaults.INGESTION_ENDPOINT);
        final URL expectedIngestionEndpointURL = new URL(Defaults.INGESTION_ENDPOINT+"/"+EndpointProvider.INGESTION_URL_PATH);
        final URL expectedLiveEndpoint = new URL(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URL_PATH);
        try {
            ConnectionString.parseInto(cs, telemetryClient);
        } catch (Exception e) {
            throw new AssertionError("Exception thrown from parse");
        }
        assertEquals(ikey, telemetryClient.getInstrumentationKey());
        assertEquals(expectedIngestionEndpoint, telemetryClient.getEndpointProvider().getIngestionEndpoint());
        assertEquals(expectedIngestionEndpointURL, telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(expectedLiveEndpoint, telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    }

    @Test
    public void emptyValueIsSameAsUnset() throws Exception {
        final String ikey = "fake-ikey";
        final String cs = "InstrumentationKey="+ikey+";EndpointSuffix=";

        ConnectionString.parseInto(cs, telemetryClient);
        assertEquals(ikey, telemetryClient.getInstrumentationKey());
        assertEquals(new URL(Defaults.INGESTION_ENDPOINT), telemetryClient.getEndpointProvider().getIngestionEndpoint());
        assertEquals(new URL(Defaults.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URL_PATH), telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(new URL(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URL_PATH), telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    }

    @Test
    public void caseInsensitiveParsing() throws Exception {
        final String ikey = "fake-ikey";
        final String live = "https://live.something.com";
        final String profiler = "https://prof.something.com";
        final String cs1 = "InstrumentationKey="+ ikey +";LiveEndpoint="+ live +";ProfilerEndpoint="+ profiler;
        final String cs2 = "instRUMentationkEY="+ ikey +";LivEEndPOINT="+ live +";ProFILErEndPOinT="+ profiler;

        TelemetryClient telemetryClient2 = new TelemetryClient();

        ConnectionString.parseInto(cs1, telemetryClient);
        ConnectionString.parseInto(cs2, telemetryClient2);

        assertEquals(telemetryClient.getInstrumentationKey(), telemetryClient2.getInstrumentationKey());
        assertEquals(telemetryClient.getEndpointProvider().getIngestionEndpoint(), telemetryClient2.getEndpointProvider().getIngestionEndpoint());
        assertEquals(telemetryClient.getEndpointProvider().getIngestionEndpointUrl(), telemetryClient2.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(telemetryClient.getEndpointProvider().getLiveEndpointUrl(), telemetryClient2.getEndpointProvider().getLiveEndpointUrl());
        assertEquals(telemetryClient.getEndpointProvider().getProfilerEndpoint(), telemetryClient2.getEndpointProvider().getProfilerEndpoint());
        assertEquals(telemetryClient.getEndpointProvider().getSnapshotEndpoint(), telemetryClient2.getEndpointProvider().getSnapshotEndpoint());
    }

    @Test
    public void orderDoesNotMatter() throws Exception {
        final String ikey = "fake-ikey";
        final String live = "https://live.something.com";
        final String profiler = "https://prof.something.com";
        final String snapshot = "https://whatever.snappy.com";
        final String cs1 = "InstrumentationKey="+ ikey +";LiveEndpoint="+ live +";ProfilerEndpoint="+ profiler+";SnapshotEndpoint="+ snapshot;
        final String cs2 = "SnapshotEndpoint="+ snapshot+";ProfilerEndpoint="+ profiler+";InstrumentationKey="+ ikey +";LiveEndpoint="+ live;

        TelemetryClient telemetryClient2 = new TelemetryClient();

        ConnectionString.parseInto(cs1, telemetryClient);
        ConnectionString.parseInto(cs2, telemetryClient2);

        assertEquals(telemetryClient.getInstrumentationKey(), telemetryClient2.getInstrumentationKey());
        assertEquals(telemetryClient.getEndpointProvider().getIngestionEndpoint(), telemetryClient2.getEndpointProvider().getIngestionEndpoint());
        assertEquals(telemetryClient.getEndpointProvider().getIngestionEndpointUrl(), telemetryClient2.getEndpointProvider().getIngestionEndpointUrl());
        assertEquals(telemetryClient.getEndpointProvider().getLiveEndpointUrl(), telemetryClient2.getEndpointProvider().getLiveEndpointUrl());
        assertEquals(telemetryClient.getEndpointProvider().getProfilerEndpoint(), telemetryClient2.getEndpointProvider().getProfilerEndpoint());
        assertEquals(telemetryClient.getEndpointProvider().getSnapshotEndpoint(), telemetryClient2.getEndpointProvider().getSnapshotEndpoint());
    }

    @Test
    public void endpointWithNoSchemeIsInvalid() {
        assertThatThrownBy(() ->
                ConnectionString.parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com", telemetryClient))
                .isInstanceOf(InvalidConnectionStringException.class)
                .hasMessageContaining("IngestionEndpoint");
    }

    @Test
    public void endpointWithPathMissingSchemeIsInvalid() throws Exception {
        assertThatThrownBy(() ->
                ConnectionString.parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com/path/prefix", telemetryClient))
                .isInstanceOf(InvalidConnectionStringException.class)
                .hasMessageContaining("IngestionEndpoint");
    }

    @Test
    public void endpointWithPortMissingSchemeIsInvalid() throws Exception {
        assertThatThrownBy(() ->
                ConnectionString.parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com:9999", telemetryClient))
                .isInstanceOf(InvalidConnectionStringException.class)
                .hasMessageContaining("IngestionEndpoint");
    }

    @Test
    public void httpEndpointKeepsScheme() throws Exception {
        ConnectionString.parseInto("InstrumentationKey=fake-ikey;IngestionEndpoint=http://my-ai.example.com", telemetryClient);
        assertEquals(new URL("http://my-ai.example.com"), telemetryClient.getEndpointProvider().getIngestionEndpoint());
    }

    @Test
    public void emptyIkeyValueIsInvalid() {
        assertThatThrownBy(() ->
                ConnectionString.parseInto("InstrumentationKey=;IngestionEndpoint=https://ingestion.example.com;EndpointSuffix=ai.example.com", telemetryClient))
                .isInstanceOf(InvalidConnectionStringException.class);
    }

    @Test
    public void emptyStringIsInvalid() {
        assertThatThrownBy(() ->
                ConnectionString.parseInto("", telemetryClient))
                .isInstanceOf(InvalidConnectionStringException.class);
    }

    @Test
    public void nonKeyValueStringIsInvalid() {
        assertThatThrownBy(() ->
                ConnectionString.parseInto(UUID.randomUUID().toString(), telemetryClient))
                .isInstanceOf(InvalidConnectionStringException.class);
    }

    @Test // when more Authorization values are available, create a copy of this test. For example, given "Authorization=Xyz", this would fail because the 'Xyz' key/value pair is missing.
    public void missingInstrumentationKeyIsInvalid() throws Exception {
        assertThatThrownBy(() ->
                ConnectionString.parseInto("LiveEndpoint=https://live.example.com", telemetryClient))
                .isInstanceOf(InvalidConnectionStringException.class);
    }

    @Test
    public void invalidUrlIsInvalidConnectionString() throws Exception {
        assertThatThrownBy(() ->
                ConnectionString.parseInto("InstrumentationKey=fake-ikey;LiveEndpoint=httpx://host", telemetryClient))
                .isInstanceOf(InvalidConnectionStringException.class)
                .hasCauseInstanceOf(MalformedURLException.class)
                .hasMessageContaining("LiveEndpoint");
    }

    @Test
    public void giantValuesAreNotAllowed() {
        String bigIkey = StringUtils.repeat('0', ConnectionString.CONNECTION_STRING_MAX_LENGTH * 2);

        assertThatThrownBy(() ->
                ConnectionString.parseInto("InstrumentationKey=" + bigIkey, telemetryClient))
                .isInstanceOf(InvalidConnectionStringException.class)
                .hasMessageContaining(Integer.toString(ConnectionString.CONNECTION_STRING_MAX_LENGTH));
    }
}
