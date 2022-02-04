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

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class StatsbeatConnectionStringTest {

  @Test
  public void testGetGeoWithoutStampSpecific() {
    String customerIngestionEndpoint = "https://fakehost-1.applicationinsights.azure.com/";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehost");

    customerIngestionEndpoint = "http://fakehost-2.example.com/";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehost");

    customerIngestionEndpoint = "https://fakehost1-3.com/";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehost1");

    customerIngestionEndpoint = "http://fakehost2-4.com/";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehost2");

    customerIngestionEndpoint = "http://www.fakehost3-5.com/";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehost3");

    customerIngestionEndpoint = "https://www.fakehostabc-6.com/";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehostabc");

    customerIngestionEndpoint = "http://fakehostabc-7.example.com/";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehostabc");

    customerIngestionEndpoint = "http://www.fakehostabc-8.example.com/";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehostabc");

    customerIngestionEndpoint = "https://fakehostabc.1-9.com/";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehostabc.1");

    customerIngestionEndpoint = "https://fakehostabc.com/";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehostabc");

    customerIngestionEndpoint = "https://fakehostabc/v2/track";
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngestionEndpoint))
        .isEqualTo("fakehostabc");
  }

  @Test
  public void testUpdateStatsbeatConnectionString() throws Exception {
    // case 1
    // customer ikey is in non-eu
    // Statsbeat config ikey is in eu
    // use Statsbeat config ikey
    TelemetryClient telemetryClient = TelemetryClient.createForTest();
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westus2-1.example.com/");
    String ikeyConfig = "00000000-0000-0000-0000-000000000001";
    String endpointConfig = "https://westeurope-2.example.com";
    ConnectionString.updateStatsbeatConnectionString(ikeyConfig, endpointConfig, telemetryClient);
    assertThat(telemetryClient.getStatsbeatInstrumentationKey()).isEqualTo(ikeyConfig);
    assertThat(telemetryClient.getEndpointProvider().getStatsbeatEndpointUrl().toString())
        .isEqualTo(endpointConfig + "/v2.1/track");

    // case 2
    // customer ikey is in non-eu
    // Statsbeat config ikey is in non-eu
    // use Statsbeat config ikey
    ikeyConfig = "00000000-0000-0000-0000-000000000002";
    endpointConfig = "https://westus2-2.example.com";
    ConnectionString.updateStatsbeatConnectionString(ikeyConfig, endpointConfig, telemetryClient);
    assertThat(telemetryClient.getStatsbeatInstrumentationKey()).isEqualTo(ikeyConfig);
    assertThat(telemetryClient.getEndpointProvider().getStatsbeatEndpointUrl().toString())
        .isEqualTo(endpointConfig + "/v2.1/track");

    // case 3
    // customer ikey is in non-eu
    // no Statsbeat config
    // use Statsbeat non-eu
    ConnectionString.updateStatsbeatConnectionString(null, null, telemetryClient);
    assertThat(telemetryClient.getStatsbeatInstrumentationKey())
        .isEqualTo(StatsbeatConnectionString.NON_EU_REGION_STATSBEAT_IKEY);
    assertThat(telemetryClient.getEndpointProvider().getStatsbeatEndpointUrl().toString())
        .isEqualTo(StatsbeatConnectionString.NON_EU_REGION_STATSBEAT_ENDPOINT + "v2.1/track");

    // case 4
    // customer is in eu
    // Statsbeat config ikey is in non-eu
    // use Statsbeat config's ikey
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000003;IngestionEndpoint=https://westeurope-1.example.com/");
    ikeyConfig = "00000000-0000-0000-0000-000000000004";
    endpointConfig = "https://westus2-4.example.com";
    ConnectionString.updateStatsbeatConnectionString(ikeyConfig, endpointConfig, telemetryClient);
    assertThat(telemetryClient.getStatsbeatInstrumentationKey()).isEqualTo(ikeyConfig);
    assertThat(telemetryClient.getEndpointProvider().getStatsbeatEndpointUrl().toString())
        .isEqualTo(endpointConfig + "/v2.1/track");

    // case 5
    // customer is in eu
    // Statsbeat config ikey is in eu
    // use Statsbeat config's ikey
    ikeyConfig = "00000000-0000-0000-0000-000000000005";
    endpointConfig = "https://francesouth-1.example.com";
    ConnectionString.updateStatsbeatConnectionString(ikeyConfig, endpointConfig, telemetryClient);
    assertThat(telemetryClient.getStatsbeatInstrumentationKey()).isEqualTo(ikeyConfig);
    assertThat(telemetryClient.getEndpointProvider().getStatsbeatEndpointUrl().toString())
        .isEqualTo(endpointConfig + "/v2.1/track");

    // case 6
    // customer is in eu
    // no statsbeat config
    // use Statsbeat eu
    ConnectionString.updateStatsbeatConnectionString(null, null, telemetryClient);
    assertThat(telemetryClient.getStatsbeatInstrumentationKey())
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);
    assertThat(telemetryClient.getEndpointProvider().getStatsbeatEndpointUrl().toString())
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_ENDPOINT + "v2.1/track");
  }

  @Test
  public void testGetInstrumentationKeyAndEndpointPairEuRegion() {
    StatsbeatConnectionString.InstrumentationKeyEndpointPair pair =
        StatsbeatConnectionString.getInstrumentationKeyAndEndpointPair(
            "https://northeurope-2.example.com/");
    assertThat(pair.instrumentationKey)
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);
    assertThat(pair.endpoint).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_ENDPOINT);
  }

  @Test
  public void testGetInstrumentationKeyAndEndpointPairNonEuRegion() {
    StatsbeatConnectionString.InstrumentationKeyEndpointPair pair =
        StatsbeatConnectionString.getInstrumentationKeyAndEndpointPair(
            "https://westus2-2.example.com/");
    assertThat(pair.instrumentationKey)
        .isEqualTo(StatsbeatConnectionString.NON_EU_REGION_STATSBEAT_IKEY);
    assertThat(pair.endpoint).isEqualTo(StatsbeatConnectionString.NON_EU_REGION_STATSBEAT_ENDPOINT);
  }
}
