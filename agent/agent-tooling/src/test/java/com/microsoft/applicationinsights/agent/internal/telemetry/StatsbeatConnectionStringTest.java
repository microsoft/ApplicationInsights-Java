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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StatsbeatConnectionStringTest {

  private TelemetryClient telemetryClient;

  @BeforeEach
  public void setup() {
    telemetryClient = TelemetryClient.createForTest();
  }

  @Test
  public void testGetGeoWithoutStampSpecific() {
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fakehost-1.applicationinsights.azure.com/");
    String customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehost");

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://fakehost-2.example.com/");
    customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehost");

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fakehost1-3.com/");
    customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehost1");

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://fakehost2-4.com/");
    customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehost2");

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://www.fakehost3-5.com/");
    customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehost3");

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://www.fakehostabc-6.com/");
    customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehostabc");

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://fakehostabc-7.example.com/");
    customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehostabc");

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://www.fakehostabc-8.example.com/");
    customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehostabc");

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fakehostabc.1-9.com/");
    customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehostabc.1");

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fakehostabc.com/");
    customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehostabc");

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fakehostabc/v2/track");
    customerIngetionEndpoint =
        telemetryClient.getEndpointProvider().getIngestionEndpoint().toString();
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(customerIngetionEndpoint))
        .isEqualTo("fakehostabc");
  }

  @Test
  public void testStatsbeatIkeyInNonEuRegion() {
    // case 1
    // customer is in non-eu
    // Statsbeat config is in eu
    // use Statsbeat config's ikey
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westus2-1.example.com/");
    String ikey = "00000000-0000-0000-0000-000000000001";
    String endpoint = "https://westeurope-1.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, telemetryClient))
        .isEqualTo(ikey);

    // case 2
    // customer is in non-eu
    // Statsbeat config is in non-eu
    // use Statsbeat config's ikey
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westus2-2.example.com/");
    ikey = "00000000-0000-0000-0000-000000000002";
    endpoint = "https://eastus-1.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, telemetryClient))
        .isEqualTo(ikey);

    // case 3
    // customer is in non-eu
    // Statsbeat config has no endpoint, i.e. doesn't know which region it is
    // use Statsbeat config's ikey
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westus2-3.example.com/");
    ikey = "00000000-0000-0000-0000-000000000003";
    endpoint = "";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, telemetryClient))
        .isEqualTo(ikey);

    // case 4
    // customer is in non-eu
    // no Statsbeat config
    // use Statsbeat non-eu
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westus2-4.example.com/");
    ikey = "";
    endpoint = "";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, telemetryClient))
        .isEqualTo(StatsbeatConnectionString.NON_EU_REGION_STATSBEAT_IKEY);
  }

  @Test
  public void testStatsbeatIkeyInEuRegion() {
    // case 1
    // customer is in eu
    // Statsbeat config is in non-eu
    // use Statsbeat eu
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westeurope-1.example.com/");
    String ikey = "00000000-0000-0000-0000-000000000001";
    String endpoint = "https://westus2-1.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    // case 2
    // customer is in eu
    // Statsbeat config is in eu
    // use Statsbeat config's ikey
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westeurope-2.example.com/");
    ikey = "00000000-0000-0000-0000-000000000002";
    endpoint = "https://northeurope-2.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, telemetryClient))
        .isEqualTo(ikey);

    // case 3
    // customer is in eu
    // Statsbeat config has no endpoint, i.e. doesn't know which region it is
    // use Statsbeat eu
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westeurope-3.example.com/");
    ikey = "00000000-0000-0000-0000-000000000003";
    endpoint = "";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    // case 4
    // customer is in eu
    // no statsbeat config
    // use Statsbeat eu
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westeurope-4.example.com/");
    ikey = "";
    endpoint = "";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);
  }

  @Test
  public void testAllEuRegionsWithoutConfig() {
    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westeurope-1.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://northeurope-2.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://francecentral-3.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://francesouth-4.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://francesouth-5.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://norwayeast-6.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://norwaywest-7.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://swedencentral-8.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://www.switzerlandnorth-9.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://switzerlandwest-10.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", telemetryClient))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);
  }
}
