package com.microsoft.applicationinsights.agent.internal.telemetry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StatsbeatConnectionStringTest {

  private TelemetryClient telemetryClient;

  @BeforeEach
  public void setup() {
    telemetryClient = TelemetryClient.createForTest();
  }

  @Test
  public void testGetGeoWithoutStampSpecific() {
    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fakehost-1.applicationinsights.azure.com/");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehost");

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://fakehost-2.example.com/");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehost");

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fakehost1-3.com/");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehost1");

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://fakehost2-4.com/");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehost2");

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://www.fakehost3-5.com/");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehost3");

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://www.fakehostabc-6.com/");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehostabc");

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://fakehostabc-7.example.com/");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehostabc");

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://www.fakehostabc-8.example.com/");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehostabc");

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fakehostabc.1-9.com/");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehostabc.1");

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fakehostabc.com/");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehostabc");

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fakehostabc/v2/track");
    assertThat(StatsbeatConnectionString.getGeoWithoutStampSpecific(telemetryClient)).isEqualTo("fakehostabc");
  }

  @Test
  public void testGetInstrumentationKey() {
    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westeurope-1.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://northeurope-2.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://francecentral-3.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://francesouth-4.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://francesouth-5.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://norwayeast-6.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://norwaywest-7.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://swedencentral-8.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://www.switzerlandnorth-9.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://switzerlandwest-10.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.EU_REGION_STATSBEAT_IKEY);

    telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://westus2-1.example.com/");
    assertThat(StatsbeatConnectionString.getInstrumentationKey(telemetryClient)).isEqualTo(StatsbeatConnectionString.NON_EU_REGION_STATSBEAT_IKEY);
  }
}
