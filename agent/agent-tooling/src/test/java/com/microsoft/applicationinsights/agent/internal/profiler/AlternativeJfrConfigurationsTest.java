// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AlternativeJfrConfigurationsTest {

  private static void assertForAllConfigs(
      Configuration.ProfilerConfiguration config, Consumer<String> assertion) {

    assertion.accept(AlternativeJfrConfigurations.getCpuProfileConfig(config).getConfiguration());

    assertion.accept(
        AlternativeJfrConfigurations.getMemoryProfileConfig(config).getConfiguration());

    assertion.accept(
        AlternativeJfrConfigurations.getManualProfileConfig(config).getConfiguration());

    assertion.accept(AlternativeJfrConfigurations.getSpanProfileConfig(config).getConfiguration());
  }

  // To be able to remove sensitive events
  @Test
  public void ifDiagnosticsAreEnabled_A_CustomJfcFileIsNotOverridden() throws IOException {
    File tmpfile = File.createTempFile("config", "jfc");
    try {
      try (BufferedWriter fw = Files.newBufferedWriter(tmpfile.toPath(), StandardCharsets.UTF_8)) {
        fw.write("a-jfc-file");
      }

      Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();

      config.cpuTriggeredSettings = tmpfile.getAbsolutePath();
      config.manualTriggeredSettings = tmpfile.getAbsolutePath();
      config.memoryTriggeredSettings = tmpfile.getAbsolutePath();

      config.enableDiagnostics = true;
      assertForAllConfigs(
          config,
          (fileContent) -> {
            Assertions.assertEquals("a-jfc-file", fileContent);
          });

    } finally {
      tmpfile.delete();
    }
  }

  @Test
  public void ifDiagnosticsAreEnabledDefaultToDiagnosticProfile() {
    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.enableDiagnostics = true;
    assertForAllConfigs(
        config,
        fileContent -> {
          Assertions.assertTrue(
              fileContent.contains("com.microsoft.applicationinsights.diagnostics.jfr.Telemetry"));
        });
  }
}
