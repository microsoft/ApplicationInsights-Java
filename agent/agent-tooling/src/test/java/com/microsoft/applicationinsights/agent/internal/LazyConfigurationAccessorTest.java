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

package com.microsoft.applicationinsights.agent.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.microsoft.applicationinsights.internal.TelemetryClient;
import org.junit.jupiter.api.Test;

class LazyConfigurationAccessorTest {

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
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(false, "true")).isTrue();
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is FALSE"
  void disableLazySetWithLazySetOptInOffEnableAgentOff() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(false, "false")).isFalse();
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is NULL"
  void disableLazySetWithLazySetOptInOffEnableAgentNull() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(false, null)).isFalse();
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is NULL, and EnableAgent
  // is TRUE"
  void disableLazySetWithLazySetOptInOffConnectionStringNullInstrumentationKeyNull() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(false, "true")).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    LazyConfigurationAccessor lazyConfigurationAccessor =
        new LazyConfigurationAccessor(telemetryClient, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(null, null);

    // then
    verify(telemetryClient, never()).setConnectionString(anyString());
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is valid, InstrumentationKey is NULL, and EnableAgent
  // is TRUE"
  void disableLazySetWithLazySetOptInOffConnectionStringNotNullInstrumentationKeyNull() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(false, "true")).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    LazyConfigurationAccessor lazyConfigurationAccessor =
        new LazyConfigurationAccessor(telemetryClient, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(CONNECTION_STRING, null);

    // then
    verify(telemetryClient).setConnectionString(CONNECTION_STRING);

    // when
    lazyConfigurationAccessor.setWebsiteSiteName(WEBSITE_SITE_NAME);

    // then
    verify(telemetryClient).setRoleName(WEBSITE_SITE_NAME);
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent
  // is TRUE")
  void enableLazySetWithLazySetOptInOffConnectionStringNullInstrumentationKeyNotNull() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(false, "true")).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    LazyConfigurationAccessor lazyConfigurationAccessor =
        new LazyConfigurationAccessor(telemetryClient, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(null, INSTRUMENTATION_KEY);

    // then
    verify(telemetryClient).setConnectionString("InstrumentationKey=" + INSTRUMENTATION_KEY);
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is TRUE"
  void enableLazySetWithLazySetOptInOnEnableAgentOn() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(true, "true")).isTrue();
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is FALSE"
  void disableLazySetWithLazySetOptInOnEnableAgentOff() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(true, "false")).isFalse();
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is NULL"
  void enableLazySetWithLazySetOptInOnEnableAgentNull() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(true, null)).isTrue();
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is NULL, InstrumentationKey is NULL, and EnableAgent is
  // TRUE"
  void disableLazySetWithLazySetOptInOnConnectionStringNullAndInstrumentationKeyNull() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(true, "true")).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    LazyConfigurationAccessor lazyConfigurationAccessor =
        new LazyConfigurationAccessor(telemetryClient, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(null, null);

    // then
    verify(telemetryClient, never()).setConnectionString(anyString());
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is valid, InstrumentationKey is NULL, and EnableAgent
  // is TRUE"
  void enableLazySetWithLazySetOptInOnConnectionStringNotNullInstrumentationKeyNull() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(false, "true")).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    LazyConfigurationAccessor lazyConfigurationAccessor =
        new LazyConfigurationAccessor(telemetryClient, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(CONNECTION_STRING, null);

    // then
    verify(telemetryClient).setConnectionString(CONNECTION_STRING);
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent
  // is TRUE"
  void enableLazySetWithLazySetOptInOnConnectionStringNullInstrumentationKeyNotNull() {
    assertThat(LazyConfigurationAccessor.shouldSetConnectionString(false, "true")).isTrue();

    // given
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    AppIdSupplier appIdSupplier = mock(AppIdSupplier.class);
    LazyConfigurationAccessor lazyConfigurationAccessor =
        new LazyConfigurationAccessor(telemetryClient, appIdSupplier);

    // when
    lazyConfigurationAccessor.setConnectionString(null, INSTRUMENTATION_KEY);

    // then
    verify(telemetryClient).setConnectionString("InstrumentationKey=" + INSTRUMENTATION_KEY);
  }
}
