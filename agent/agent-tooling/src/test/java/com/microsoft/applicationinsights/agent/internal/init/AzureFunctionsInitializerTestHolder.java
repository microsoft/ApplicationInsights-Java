// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AzureFunctionsInitializerTestHolder {

  /*
   * Lazily Set Connection String For Linux Consumption Plan:
   *
   *    Term      LazySetOptIn   EnableAgent        LazySet
   *    Preview   FALSE          TRUE               Enabled
   *                             FALSE              Disabled
   *                             NULL               Disabled
   *    GA        TRUE           TRUE               Enabled
   *                             FALSE              Disabled
   *                             NULL               Enabled
   */

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
  void disableLazySetWithLazySetOptInOff() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", false)).isTrue();
  }

  @Test
  // "LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent
  // is TRUE")
  void enableLazySetWithLazySetOptInOff() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", false)).isTrue();
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
  void disableLazySetWithLazySetOptInOn() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", true)).isTrue();
  }

  @Test
  // "LazySetOptIn is TRUE, ConnectionString is valid, InstrumentationKey is NULL, and EnableAgent
  // is TRUE"
  void enableLazySetWithLazySetOptInOn() {
    assertThat(AzureFunctionsInitializer.isAgentEnabled("true", false)).isTrue();
  }
}
