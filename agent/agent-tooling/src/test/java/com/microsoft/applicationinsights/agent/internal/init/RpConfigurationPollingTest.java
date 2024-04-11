// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.SamplingTestUtil;
import com.microsoft.applicationinsights.agent.internal.classicsdk.BytecodeUtilImpl;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfiguration;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RpConfigurationPollingTest {

  public static final Map<String, String> envVars = new HashMap<>();

  @BeforeEach
  void beforeEach() {
    envVars.clear();
    // default sampler at startup is "Sampler.alwaysOff()", and this test relies on real sampler
    Configuration config = new Configuration();
    config.sampling.percentage = 100.0;
    DelegatingSampler.getInstance()
        .setDelegate(Samplers.getSampler(config.sampling, config.preview.sampling));
  }

  @AfterEach
  void afterEach() {
    // need to reset trace config back to default (with default sampler)
    // otherwise tests run after this can fail
    Configuration config = new Configuration();
    config.sampling.percentage = 100.0;
    DelegatingSampler.getInstance()
        .setDelegate(Samplers.getSampler(config.sampling, config.preview.sampling));
  }

  @Test
  void shouldUpdate() throws URISyntaxException {
    // given
    Configuration config = new Configuration();
    config.sampling.percentage = 50.0;

    BytecodeUtilImpl.samplingPercentage = 50;

    TelemetryClient telemetryClient = TelemetryClient.createForTest();
    telemetryClient.updateConnectionStrings(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000", null, null);

    RpConfiguration rpConfiguration = new RpConfiguration();
    rpConfiguration.connectionString = "InstrumentationKey=00000000-0000-0000-0000-000000000000";
    rpConfiguration.sampling.percentage = 50.0;
    rpConfiguration.configPath =
        Paths.get(
            RpConfigurationPollingTest.class.getResource("/applicationinsights-rp.json").toURI());
    rpConfiguration.lastModifiedTime = 0;

    // pre-check
    assertThat(telemetryClient.getInstrumentationKey())
        .isEqualTo("00000000-0000-0000-0000-000000000000");
    assertThat(BytecodeUtilImpl.samplingPercentage).isEqualTo(50);
    assertThat(getCurrentSamplingPercentage()).isNull();

    // when
    RuntimeConfigurator runtimeConfigurator =
        new RuntimeConfigurator(telemetryClient, () -> null, config, item -> {}, null);
    new RpConfigurationPolling(rpConfiguration, runtimeConfigurator).run();

    // then
    assertThat(telemetryClient.getInstrumentationKey())
        .isEqualTo("11111111-1111-1111-1111-111111111111");
    // TODO (trask) fix, this should really be 10
    assertThat(BytecodeUtilImpl.samplingPercentage).isEqualTo(9.9f);
    assertThat(getCurrentSamplingPercentage()).isEqualTo(10);
  }

  @Test
  void shouldBePopulatedByEnvVars() throws URISyntaxException {
    // given
    Configuration config = new Configuration();
    config.sampling.percentage = 50.0;

    BytecodeUtilImpl.samplingPercentage = 50;

    TelemetryClient telemetryClient = TelemetryClient.createForTest();
    telemetryClient.updateConnectionStrings(
        "InstrumentationKey=00000000-0000-0000-0000-000000000000", null, null);

    RpConfiguration rpConfiguration = new RpConfiguration();
    rpConfiguration.connectionString = "InstrumentationKey=00000000-0000-0000-0000-000000000000";
    rpConfiguration.sampling.percentage = 50.0;
    rpConfiguration.configPath =
        Paths.get(
            RpConfigurationPollingTest.class.getResource("/applicationinsights-rp.json").toURI());
    rpConfiguration.lastModifiedTime = 0;

    envVars.put(
        "APPLICATIONINSIGHTS_CONNECTION_STRING",
        "InstrumentationKey=22222222-2222-2222-2222-222222222222");
    envVars.put("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", "24");

    // pre-check
    assertThat(telemetryClient.getInstrumentationKey())
        .isEqualTo("00000000-0000-0000-0000-000000000000");
    assertThat(BytecodeUtilImpl.samplingPercentage).isEqualTo(50);
    assertThat(getCurrentSamplingPercentage()).isNull();

    // when
    RuntimeConfigurator runtimeConfigurator =
        new RuntimeConfigurator(telemetryClient, () -> null, config, item -> {}, null);
    new RpConfigurationPolling(rpConfiguration, runtimeConfigurator).run();

    // then
    assertThat(telemetryClient.getInstrumentationKey())
        .isEqualTo("22222222-2222-2222-2222-222222222222");
    // TODO (trask) fix, this should really be 24
    assertThat(BytecodeUtilImpl.samplingPercentage).isEqualTo(24);
    assertThat(getCurrentSamplingPercentage()).isEqualTo(25);
  }

  @Nullable
  private static Double getCurrentSamplingPercentage() {
    return SamplingTestUtil.getCurrentSamplingPercentage(DelegatingSampler.getInstance());
  }
}
