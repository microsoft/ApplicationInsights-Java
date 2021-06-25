/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.wascore.config.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.agent.internal.wascore.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.wascore.config.connection.ConnectionString.Defaults;
import com.microsoft.applicationinsights.agent.internal.wascore.config.connection.ConnectionString.EndpointPrefixes;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

class ConnectionStringParsingTests {

  private final TelemetryClient telemetryClient = new TelemetryClient();

  @Test
  void minimalString() throws Exception {
    final String ikey = "fake-ikey";
    final String cs = "InstrumentationKey=" + ikey;

    ConnectionString.parseInto(cs, telemetryClient);
    assertThat(telemetryClient.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(new URL(Defaults.INGESTION_ENDPOINT));
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(
            new URL(Defaults.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URL_PATH));
    assertThat(telemetryClient.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(new URL(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URL_PATH));
  }

  @Test // this test does not use this.config
  void appIdUrlIsConstructedWithIkeyFromIngestionEndpoint() throws MalformedURLException {
    EndpointProvider ep = new EndpointProvider();
    String ikey = "fake-ikey";
    final String host = "http://123.com";
    ep.setIngestionEndpoint(new URL(host));
    assertThat(ep.getAppIdEndpointUrl(ikey))
        .isEqualTo(
            new URL(
                host
                    + "/"
                    + EndpointProvider.API_PROFILES_APP_ID_URL_PREFIX
                    + ikey
                    + EndpointProvider.API_PROFILES_APP_ID_URL_SUFFIX));
  }

  @Test
  void appIdUrlWithPathKeepsIt() throws MalformedURLException {
    EndpointProvider ep = new EndpointProvider();
    String ikey = "fake-ikey";
    String url = "http://123.com/path/321";
    ep.setIngestionEndpoint(new URL(url));
    assertThat(ep.getAppIdEndpointUrl(ikey))
        .isEqualTo(
            new URL(
                url
                    + "/"
                    + EndpointProvider.API_PROFILES_APP_ID_URL_PREFIX
                    + ikey
                    + EndpointProvider.API_PROFILES_APP_ID_URL_SUFFIX));

    ep.setIngestionEndpoint(new URL(url + "/"));
    assertThat(ep.getAppIdEndpointUrl(ikey))
        .isEqualTo(
            new URL(
                url
                    + "/"
                    + EndpointProvider.API_PROFILES_APP_ID_URL_PREFIX
                    + ikey
                    + EndpointProvider.API_PROFILES_APP_ID_URL_SUFFIX));
  }

  @Test
  void ikeyWithSuffix() throws Exception {
    final String ikey = "fake-ikey";
    final String suffix = "ai.example.com";
    final String cs = "InstrumentationKey=" + ikey + ";EndpointSuffix=" + suffix;
    URL expectedIngestionEndpoint =
        new URL("https://" + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX + "." + suffix);
    URL expectedIngestionEndpointUrl =
        new URL(
            "https://"
                + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(
            "https://"
                + EndpointPrefixes.LIVE_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.LIVE_URL_PATH);

    ConnectionString.parseInto(cs, telemetryClient);
    assertThat(telemetryClient.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(expectedIngestionEndpoint);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(expectedIngestionEndpointUrl);
    assertThat(telemetryClient.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void suffixWithPathRetainsThePath() throws Exception {
    final String ikey = "fake-ikey";
    final String suffix = "ai.example.com/my-proxy-app/doProxy";
    final String cs = "InstrumentationKey=" + ikey + ";EndpointSuffix=" + suffix;
    URL expectedIngestionEndpoint =
        new URL("https://" + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX + "." + suffix);
    URL expectedIngestionEndpointUrl =
        new URL(
            "https://"
                + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(
            "https://"
                + EndpointPrefixes.LIVE_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.LIVE_URL_PATH);

    ConnectionString.parseInto(cs, telemetryClient);
    assertThat(telemetryClient.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(expectedIngestionEndpoint);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(expectedIngestionEndpointUrl);
    assertThat(telemetryClient.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void suffixSupportsPort() throws Exception {
    final String ikey = "fake-ikey";
    final String suffix = "ai.example.com:9999";
    final String cs = "InstrumentationKey=" + ikey + ";EndpointSuffix=" + suffix;
    URL expectedIngestionEndpoint =
        new URL("https://" + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX + "." + suffix);
    URL expectedIngestionEndpointUrl =
        new URL(
            "https://"
                + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(
            "https://"
                + EndpointPrefixes.LIVE_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.LIVE_URL_PATH);

    ConnectionString.parseInto(cs, telemetryClient);
    assertThat(telemetryClient.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(expectedIngestionEndpoint);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(expectedIngestionEndpointUrl);
    assertThat(telemetryClient.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void ikeyWithExplicitEndpoints() throws Exception {
    final String ikey = "fake-ikey";
    URL expectedIngestionEndpoint = new URL("https://ingestion.example.com");
    URL expectedIngestionEndpointUrl =
        new URL("https://ingestion.example.com/" + EndpointProvider.INGESTION_URL_PATH);
    final String liveHost = "https://live.example.com";
    URL expectedLiveEndpoint = new URL(liveHost + "/" + EndpointProvider.LIVE_URL_PATH);
    String cs =
        "InstrumentationKey="
            + ikey
            + ";IngestionEndpoint="
            + expectedIngestionEndpoint
            + ";LiveEndpoint="
            + liveHost;

    ConnectionString.parseInto(cs, telemetryClient);
    assertThat(telemetryClient.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(expectedIngestionEndpoint);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(expectedIngestionEndpointUrl);
    assertThat(telemetryClient.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void explicitEndpointOverridesSuffix() throws Exception {
    final String ikey = "fake-ikey";
    final String suffix = "ai.example.com";
    URL expectedIngestionEndpoint = new URL("https://ingestion.example.com");
    URL expectedIngestionEndpointUrl =
        new URL("https://ingestion.example.com/" + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(
            "https://"
                + EndpointPrefixes.LIVE_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.LIVE_URL_PATH);
    String cs =
        "InstrumentationKey="
            + ikey
            + ";IngestionEndpoint="
            + expectedIngestionEndpoint
            + ";EndpointSuffix="
            + suffix;

    ConnectionString.parseInto(cs, telemetryClient);
    assertThat(telemetryClient.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(expectedIngestionEndpoint);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(expectedIngestionEndpointUrl);
    assertThat(telemetryClient.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void emptyPairIsIgnored() throws MalformedURLException, InvalidConnectionStringException {
    final String ikey = "fake-ikey";
    final String suffix = "ai.example.com";
    final String cs = "InstrumentationKey=" + ikey + ";;EndpointSuffix=" + suffix + ";";
    URL expectedIngestionEndpoint =
        new URL("https://" + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX + "." + suffix);
    URL expectedIngestionEndpointUrl =
        new URL(
            "https://"
                + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(
            "https://"
                + EndpointPrefixes.LIVE_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.LIVE_URL_PATH);
    ConnectionString.parseInto(cs, telemetryClient);
    assertThat(telemetryClient.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(expectedIngestionEndpoint);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(expectedIngestionEndpointUrl);
    assertThat(telemetryClient.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void emptyKeyIsIgnored() throws MalformedURLException, InvalidConnectionStringException {
    final String ikey = "fake-ikey";
    final String cs = "InstrumentationKey=" + ikey + ";=1234";
    URL expectedIngestionEndpoint = new URL(Defaults.INGESTION_ENDPOINT);
    URL expectedIngestionEndpointUrl =
        new URL(Defaults.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URL_PATH);
    ConnectionString.parseInto(cs, telemetryClient);
    assertThat(telemetryClient.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(expectedIngestionEndpoint);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(expectedIngestionEndpointUrl);
    assertThat(telemetryClient.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void emptyValueIsSameAsUnset() throws Exception {
    final String ikey = "fake-ikey";
    final String cs = "InstrumentationKey=" + ikey + ";EndpointSuffix=";

    ConnectionString.parseInto(cs, telemetryClient);
    assertThat(telemetryClient.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(new URL(Defaults.INGESTION_ENDPOINT));
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(
            new URL(Defaults.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URL_PATH));
    assertThat(telemetryClient.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(new URL(Defaults.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URL_PATH));
  }

  @Test
  void caseInsensitiveParsing() throws Exception {
    final String ikey = "fake-ikey";
    final String live = "https://live.something.com";
    final String profiler = "https://prof.something.com";
    final String cs1 =
        "InstrumentationKey=" + ikey + ";LiveEndpoint=" + live + ";ProfilerEndpoint=" + profiler;
    final String cs2 =
        "instRUMentationkEY=" + ikey + ";LivEEndPOINT=" + live + ";ProFILErEndPOinT=" + profiler;

    TelemetryClient telemetryClient2 = new TelemetryClient();

    ConnectionString.parseInto(cs1, telemetryClient);
    ConnectionString.parseInto(cs2, telemetryClient2);

    assertThat(telemetryClient2.getInstrumentationKey())
        .isEqualTo(telemetryClient.getInstrumentationKey());
    assertThat(telemetryClient2.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(telemetryClient.getEndpointProvider().getIngestionEndpoint());
    assertThat(telemetryClient2.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
    assertThat(telemetryClient2.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    assertThat(telemetryClient2.getEndpointProvider().getProfilerEndpoint())
        .isEqualTo(telemetryClient.getEndpointProvider().getProfilerEndpoint());
    assertThat(telemetryClient2.getEndpointProvider().getSnapshotEndpoint())
        .isEqualTo(telemetryClient.getEndpointProvider().getSnapshotEndpoint());
  }

  @Test
  void orderDoesNotMatter() throws Exception {
    final String ikey = "fake-ikey";
    final String live = "https://live.something.com";
    final String profiler = "https://prof.something.com";
    final String snapshot = "https://whatever.snappy.com";
    final String cs1 =
        "InstrumentationKey="
            + ikey
            + ";LiveEndpoint="
            + live
            + ";ProfilerEndpoint="
            + profiler
            + ";SnapshotEndpoint="
            + snapshot;
    final String cs2 =
        "SnapshotEndpoint="
            + snapshot
            + ";ProfilerEndpoint="
            + profiler
            + ";InstrumentationKey="
            + ikey
            + ";LiveEndpoint="
            + live;

    TelemetryClient telemetryClient2 = new TelemetryClient();

    ConnectionString.parseInto(cs1, telemetryClient);
    ConnectionString.parseInto(cs2, telemetryClient2);

    assertThat(telemetryClient2.getInstrumentationKey())
        .isEqualTo(telemetryClient.getInstrumentationKey());
    assertThat(telemetryClient2.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(telemetryClient.getEndpointProvider().getIngestionEndpoint());
    assertThat(telemetryClient2.getEndpointProvider().getIngestionEndpointUrl())
        .isEqualTo(telemetryClient.getEndpointProvider().getIngestionEndpointUrl());
    assertThat(telemetryClient2.getEndpointProvider().getLiveEndpointUrl())
        .isEqualTo(telemetryClient.getEndpointProvider().getLiveEndpointUrl());
    assertThat(telemetryClient2.getEndpointProvider().getProfilerEndpoint())
        .isEqualTo(telemetryClient.getEndpointProvider().getProfilerEndpoint());
    assertThat(telemetryClient2.getEndpointProvider().getSnapshotEndpoint())
        .isEqualTo(telemetryClient.getEndpointProvider().getSnapshotEndpoint());
  }

  @Test
  void endpointWithNoSchemeIsInvalid() {
    assertThatThrownBy(
            () ->
                ConnectionString.parseInto(
                    "InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com",
                    telemetryClient))
        .isInstanceOf(InvalidConnectionStringException.class)
        .hasMessageContaining("IngestionEndpoint");
  }

  @Test
  void endpointWithPathMissingSchemeIsInvalid() {
    assertThatThrownBy(
            () ->
                ConnectionString.parseInto(
                    "InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com/path/prefix",
                    telemetryClient))
        .isInstanceOf(InvalidConnectionStringException.class)
        .hasMessageContaining("IngestionEndpoint");
  }

  @Test
  void endpointWithPortMissingSchemeIsInvalid() {
    assertThatThrownBy(
            () ->
                ConnectionString.parseInto(
                    "InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com:9999",
                    telemetryClient))
        .isInstanceOf(InvalidConnectionStringException.class)
        .hasMessageContaining("IngestionEndpoint");
  }

  @Test
  void httpEndpointKeepsScheme() throws Exception {
    ConnectionString.parseInto(
        "InstrumentationKey=fake-ikey;IngestionEndpoint=http://my-ai.example.com", telemetryClient);
    assertThat(telemetryClient.getEndpointProvider().getIngestionEndpoint())
        .isEqualTo(new URL("http://my-ai.example.com"));
  }

  @Test
  void emptyIkeyValueIsInvalid() {
    assertThatThrownBy(
            () ->
                ConnectionString.parseInto(
                    "InstrumentationKey=;IngestionEndpoint=https://ingestion.example.com;EndpointSuffix=ai.example.com",
                    telemetryClient))
        .isInstanceOf(InvalidConnectionStringException.class);
  }

  @Test
  void emptyStringIsInvalid() {
    assertThatThrownBy(() -> ConnectionString.parseInto("", telemetryClient))
        .isInstanceOf(InvalidConnectionStringException.class);
  }

  @Test
  void nonKeyValueStringIsInvalid() {
    assertThatThrownBy(
            () -> ConnectionString.parseInto(UUID.randomUUID().toString(), telemetryClient))
        .isInstanceOf(InvalidConnectionStringException.class);
  }

  @Test // when more Authorization values are available, create a copy of this test. For example,
  // given "Authorization=Xyz", this would fail because the 'Xyz' key/value pair is missing.
  void missingInstrumentationKeyIsInvalid() {
    assertThatThrownBy(
            () ->
                ConnectionString.parseInto(
                    "LiveEndpoint=https://live.example.com", telemetryClient))
        .isInstanceOf(InvalidConnectionStringException.class);
  }

  @Test
  void invalidUrlIsInvalidConnectionString() {
    assertThatThrownBy(
            () ->
                ConnectionString.parseInto(
                    "InstrumentationKey=fake-ikey;LiveEndpoint=httpx://host", telemetryClient))
        .isInstanceOf(InvalidConnectionStringException.class)
        .hasCauseInstanceOf(MalformedURLException.class)
        .hasMessageContaining("LiveEndpoint");
  }

  @Test
  void giantValuesAreNotAllowed() {
    String bigIkey = StringUtils.repeat('0', ConnectionString.CONNECTION_STRING_MAX_LENGTH * 2);

    assertThatThrownBy(
            () -> ConnectionString.parseInto("InstrumentationKey=" + bigIkey, telemetryClient))
        .isInstanceOf(InvalidConnectionStringException.class)
        .hasMessageContaining(Integer.toString(ConnectionString.CONNECTION_STRING_MAX_LENGTH));
  }
}
