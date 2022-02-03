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
  public void testStatsbeatIkeyInNonEuRegion() {
    // case 1
    // customer is in non-eu
    // Statsbeat config is in eu
    // use Statsbeat config's ikey
    String customerEndpoint = "https://westus2-1.example.com/";
    String ikey = "00000000-0000-0000-0000-000000000001";
    String endpoint = "https://westeurope-1.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, customerEndpoint))
        .isEqualTo(ikey);

    // case 2
    // customer is in non-eu
    // Statsbeat config is in non-eu
    // use Statsbeat config's ikey
    customerEndpoint = "https://westus2-2.example.com/";
    ikey = "00000000-0000-0000-0000-000000000002";
    endpoint = "https://eastus-1.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, customerEndpoint))
        .isEqualTo(ikey);

    // case 3
    // customer is in non-eu
    // Statsbeat config has no endpoint, i.e. doesn't know which region it is
    // use Statsbeat config's ikey
    customerEndpoint = "https://westus2-3.example.com/";
    ikey = "00000000-0000-0000-0000-000000000003";
    endpoint = "";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, customerEndpoint))
        .isEqualTo(ikey);

    // case 4
    // customer is in non-eu
    // no Statsbeat config
    // use Statsbeat non-eu
    customerEndpoint = "https://westus2-4.example.com/";
    ikey = "";
    endpoint = "";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.NON_EU_REGION_STATSBEAT_IKEY);
  }

  @Test
  public void testStatsbeatIkeyInEuRegion() {
    // case 1
    // customer is in eu
    // Statsbeat config is in non-eu
    // use Statsbeat eu
    String customerEndpoint = "https://westeurope-1.example.com/";
    String ikey = "00000000-0000-0000-0000-000000000001";
    String endpoint = "https://westus2-1.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    // case 2
    // customer is in eu
    // Statsbeat config is in eu
    // use Statsbeat config's ikey
    customerEndpoint = "https://westeurope-2.example.com/";
    ikey = "00000000-0000-0000-0000-000000000002";
    endpoint = "https://northeurope-2.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, customerEndpoint))
        .isEqualTo(ikey);

    // case 3
    // customer is in eu
    // Statsbeat config has no endpoint, i.e. doesn't know which region it is
    // use Statsbeat eu
    customerEndpoint = "https://westeurope-3.example.com/";
    ikey = "00000000-0000-0000-0000-000000000003";
    endpoint = "";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    // case 4
    // customer is in eu
    // no statsbeat config
    // use Statsbeat eu
    customerEndpoint = "https://westeurope-4.example.com/";
    ikey = "";
    endpoint = "";
    assertThat(StatsbeatConnectionString.getInstrumentationKey(ikey, endpoint, customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);
  }

  @Test
  public void testAllEuRegionsWithoutConfig() {
    String customerEndpoint = "https://westeurope-1.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    customerEndpoint = "https://northeurope-2.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    customerEndpoint = "https://francecentral-3.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    customerEndpoint = "https://francesouth-4.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    customerEndpoint = "https://francesouth-5.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    customerEndpoint = "https://norwayeast-6.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    customerEndpoint = "https://norwaywest-7.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    customerEndpoint = "https://swedencentral-8.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    customerEndpoint = "https://www.switzerlandnorth-9.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    customerEndpoint = "https://switzerlandwest-10.example.com/";
    assertThat(StatsbeatConnectionString.getInstrumentationKey("", "", customerEndpoint))
        .isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);
  }
}
