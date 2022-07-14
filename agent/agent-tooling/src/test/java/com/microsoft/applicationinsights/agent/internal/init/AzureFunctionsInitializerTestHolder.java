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

package com.microsoft.applicationinsights.agent.internal.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    verify(telemetryClient, never()).setConnectionString(any());
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
    verify(telemetryClient)
        .setConnectionString(argThat(cs -> cs.getInstrumentationKey().equals(INSTRUMENTATION_KEY)));

    // when
    lazyConfigurationAccessor.setWebsiteSiteName(WEBSITE_SITE_NAME);

    // then
    verify(telemetryClient).setRoleName(WEBSITE_SITE_NAME);
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
    verify(telemetryClient)
        .setConnectionString(argThat(cs -> cs.getInstrumentationKey().equals(INSTRUMENTATION_KEY)));
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
    verify(telemetryClient, never()).setConnectionString(any());
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
    verify(telemetryClient)
        .setConnectionString(argThat(cs -> cs.getInstrumentationKey().equals(INSTRUMENTATION_KEY)));
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
    verify(telemetryClient)
        .setConnectionString(argThat(cs -> cs.getInstrumentationKey().equals(INSTRUMENTATION_KEY)));
  }
}
