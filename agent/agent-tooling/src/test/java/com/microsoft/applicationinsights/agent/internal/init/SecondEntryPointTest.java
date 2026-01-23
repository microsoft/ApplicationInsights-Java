// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SecondEntryPointTest {

  // Test cases matching the spec table in
  // https://github.com/aep-health-and-standards/Telemetry-Collection-Spec/blob/main/ApplicationInsights/AutoAttach_Env_Vars.md#metrics-exporter
  //
  // OTEL_METRICS_EXPORTER | AMLE   | azure_monitor included
  // ----------------------|--------|------------------------
  static Stream<Arguments> metricsExporterSpecTable() {
    return Stream.of(
        // AMLE unset
        Arguments.of(null, null, true),
        Arguments.of("none", null, false),
        Arguments.of("azure_monitor", null, true),
        Arguments.of("otlp,azure_monitor", null, true),
        Arguments.of("otlp", null, false),
        // AMLE=true (always include azure_monitor)
        Arguments.of(null, "true", true),
        Arguments.of("none", "true", true),
        Arguments.of("azure_monitor", "true", true),
        Arguments.of("otlp,azure_monitor", "true", true),
        Arguments.of("otlp", "true", true),
        // AMLE=false (same as unset)
        Arguments.of(null, "false", true),
        Arguments.of("none", "false", false),
        Arguments.of("azure_monitor", "false", true),
        Arguments.of("otlp,azure_monitor", "false", true),
        Arguments.of("otlp", "false", false));
  }

  @ParameterizedTest(name = "exporter={0}, amle={1} -> included={2}")
  @MethodSource("metricsExporterSpecTable")
  void testUpdateMetricsExporter(String exporter, String amle, boolean expectAzureMonitor) {
    String result = SecondEntryPoint.conditionallyAddAzureMonitorExporter(exporter, amle);
    assertThat(SecondEntryPoint.containsAzureMonitor(result)).isEqualTo(expectAzureMonitor);
  }
}
