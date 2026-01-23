// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AiConfigCustomizerTest {

  @Test
  void isAksAttach() {
    assertThat(AiConfigCustomizer.isAksAttach("dummy-aks-namespace")).isTrue();

    assertThat(AiConfigCustomizer.isAksAttach(null)).isFalse();
    assertThat(AiConfigCustomizer.isAksAttach("")).isFalse();
  }

  @Test
  void updateMetricsExporter_ExporterUnset() {

    assertThat(AiConfigCustomizer.updateMetricsExporter(null, null))
        .isEqualTo("azure_monitor,otlp");

    assertThat(AiConfigCustomizer.updateMetricsExporter("", null))
        .isEqualTo("azure_monitor,otlp");
    
    assertThat(AiConfigCustomizer.updateMetricsExporter(null, "true"))
        .isEqualTo("azure_monitor,otlp");
    
    assertThat(AiConfigCustomizer.updateMetricsExporter(null, "True"))
        .isEqualTo("azure_monitor,otlp");
  }

  static Stream<Arguments> stringPairs() {
    return Stream.of(
        Arguments.of("none",               "none"),
        Arguments.of("azure_monitor",      "azure_monitor,otlp"),
        Arguments.of("azure_monitor,otlp", "azure_monitor,otlp"),
        Arguments.of("otlp",               "otlp"));
    }
  
  @ParameterizedTest
  @MethodSource("stringPairs")
  void updateMetricsExporter_ExporterSet_AMLE_Unset(String metricsExporter, String expectAzureMonitor) {
    assertThat(AiConfigCustomizer.updateMetricsExporter(metricsExporter, null))
        .isEqualTo(expectAzureMonitor);
    assertThat(AiConfigCustomizer.updateMetricsExporter(metricsExporter, ""))
        .isEqualTo(expectAzureMonitor);
  }
  
  static Stream<Arguments> stringPairs2() {
    return Stream.of(
        Arguments.of("none",                "azure_monitor,otlp"),
        Arguments.of("azure_monitor",       "azure_monitor,otlp"),
        Arguments.of("azure_monitor,otlp",  "azure_monitor,otlp"),
        Arguments.of("otlp",                "otlp,azure_monitor"));
    }
  
  @ParameterizedTest
  @MethodSource("stringPairs2")
  void updateMetricsExporter_ExporterSet_AMLE_True(String metricsExporter, String expectAzureMonitor) {
    assertThat(AiConfigCustomizer.updateMetricsExporter(metricsExporter, "true"))
        .isEqualTo(expectAzureMonitor);
    assertThat(AiConfigCustomizer.updateMetricsExporter(metricsExporter, "True"))
        .isEqualTo(expectAzureMonitor);
  }

  static Stream<Arguments> stringPairs3() {
    return Stream.of(
        Arguments.of("none",                "none"),
        Arguments.of("azure_monitor",       "azure_monitor"),
        Arguments.of("azure_monitor,otlp",  "azure_monitor"),
        Arguments.of("otlp",                "otlp"));
    }
  
  @ParameterizedTest
  @MethodSource("stringPairs3")
  void updateMetricsExporter_ExporterSet_AMLE_False(String metricsExporter, String expectAzureMonitor) {
    assertThat(AiConfigCustomizer.updateMetricsExporter(metricsExporter, "false"))
        .isEqualTo(expectAzureMonitor);
    assertThat(AiConfigCustomizer.updateMetricsExporter(metricsExporter, "False"))
        .isEqualTo(expectAzureMonitor);
  }
}
