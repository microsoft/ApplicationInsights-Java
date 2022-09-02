// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.microsoft.applicationinsights.agent.internal.exporter.AgentLogExporter;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import org.junit.jupiter.api.Test;

class AzureFunctionsInitializerTestHolder {

  /*
   * Lazily Set Connection String For Linux Consumption Plan:
   *
   *    Term      LazySetOptIn   ConnectionString      EnableAgent        LazySet
   *    Preview   FALSE          VALID                 TRUE               Enabled
   *                             VALID                 FALSE              Disabled
   *                             VALID                 NULL               Disabled
   *                             NULL                  TRUE/FALSE/NULL    Disabled
   *    GA        TRUE           VALID                 TRUE               Enabled
   *                             VALID                 FALSE              Disabled
   *                             VALID                 NULL               Enabled
   *                             NULL                  TRUE/FALSE/NULL    Disabled
   */
  private static final String CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-0FEEDDADBEEF";

  private static final String INSTRUMENTATION_KEY = "00000000-0000-0000-0000-0FEEDDADBEEF";
  private static final String WEBSITE_SITE_NAME = "fake_site_name";

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is TRUE"
  void enableLazySetWithLazySetOptInOffEnableAgentOn() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", false)).isTrue();
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is FALSE"
  void disableLazySetWithLazySetOptInOffEnableAgentOff() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("false", false)).isFalse();
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is NULL"
  void disableLazySetWithLazySetOptInOffEnableAgentNull() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled(null, false)).isFalse();
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is NULL, and EnableAgent
  // is TRUE"
  void disableLazySetWithLazySetOptInOffConnectionStringNullInstrumentationKeyNull() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", false)).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AgentLogExporter agentLogExporter = mock(AgentLogExporter.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    AzureFunctionsInitializer lazyConfigurationAccessor =
        new AzureFunctionsInitializer(telemetryClient, agentLogExporter, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(null, null);

    // then
    verify(telemetryClient, never()).updateConnectionStrings(any(), any(), any());
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is valid, InstrumentationKey is NULL, and EnableAgent
  // is TRUE"
  void disableLazySetWithLazySetOptInOffConnectionStringNotNullInstrumentationKeyNull() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", false)).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AgentLogExporter agentLogExporter = mock(AgentLogExporter.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    AzureFunctionsInitializer lazyConfigurationAccessor =
        new AzureFunctionsInitializer(telemetryClient, agentLogExporter, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(CONNECTION_STRING, null);

    // then
    verify(telemetryClient).updateConnectionStrings(CONNECTION_STRING, null, null);

    // when
    lazyConfigurationAccessor.setWebsiteSiteName(WEBSITE_SITE_NAME);

    // then
    verify(telemetryClient).updateRoleName(WEBSITE_SITE_NAME);
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent
  // is TRUE")
  void enableLazySetWithLazySetOptInOffConnectionStringNullInstrumentationKeyNotNull() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", false)).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AgentLogExporter agentLogExporter = mock(AgentLogExporter.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    AzureFunctionsInitializer lazyConfigurationAccessor =
        new AzureFunctionsInitializer(telemetryClient, agentLogExporter, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(null, INSTRUMENTATION_KEY);

    // then
    verify(telemetryClient).updateConnectionStrings(CONNECTION_STRING, null, null);
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is TRUE"
  void enableLazySetWithLazySetOptInOnEnableAgentOn() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", true)).isTrue();
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is FALSE"
  void disableLazySetWithLazySetOptInOnEnableAgentOff() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("false", true)).isFalse();
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is NULL"
  void enableLazySetWithLazySetOptInOnEnableAgentNull() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled(null, true)).isTrue();
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is NULL, InstrumentationKey is NULL, and EnableAgent is
  // TRUE"
  void disableLazySetWithLazySetOptInOnConnectionStringNullAndInstrumentationKeyNull() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", true)).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AgentLogExporter agentLogExporter = mock(AgentLogExporter.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    AzureFunctionsInitializer lazyConfigurationAccessor =
        new AzureFunctionsInitializer(telemetryClient, agentLogExporter, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(null, null);

    // then
    verify(telemetryClient, never()).updateConnectionStrings(any(), any(), any());
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is valid, InstrumentationKey is NULL, and EnableAgent
  // is TRUE"
  void enableLazySetWithLazySetOptInOnConnectionStringNotNullInstrumentationKeyNull() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", false)).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AgentLogExporter agentLogExporter = mock(AgentLogExporter.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    AzureFunctionsInitializer lazyConfigurationAccessor =
        new AzureFunctionsInitializer(telemetryClient, agentLogExporter, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(CONNECTION_STRING, null);

    // then
    verify(telemetryClient).updateConnectionStrings(CONNECTION_STRING, null, null);
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent
  // is TRUE"
  void enableLazySetWithLazySetOptInOnConnectionStringNullInstrumentationKeyNotNull() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", false)).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AgentLogExporter agentLogExporter = mock(AgentLogExporter.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    AzureFunctionsInitializer lazyConfigurationAccessor =
        new AzureFunctionsInitializer(telemetryClient, agentLogExporter, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(null, INSTRUMENTATION_KEY);

    // then
    verify(telemetryClient).updateConnectionStrings(CONNECTION_STRING, null, null);
  }
}
