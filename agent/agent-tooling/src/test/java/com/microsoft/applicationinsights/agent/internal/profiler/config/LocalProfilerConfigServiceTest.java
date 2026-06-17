// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;

class LocalProfilerConfigServiceTest {

  @TempDir File tempDir;

  @Test
  void returnsEmptyWhenNoConfigFileExists() {
    LocalProfilerConfigService service = new LocalProfilerConfigService(tempDir);

    Mono<ProfilerConfiguration> result = service.pullSettings();

    assertThat(result.blockOptional()).isEmpty();
    assertThat(service.isLocalConfigPresent()).isFalse();
  }

  @Test
  void returnsConfigWhenValidFileExists() throws IOException {
    writeConfigFile(validConfigJson());
    LocalProfilerConfigService service = new LocalProfilerConfigService(tempDir);

    ProfilerConfiguration config = service.pullSettings().block();

    assertThat(config).isNotNull();
    assertThat(config.isEnabled()).isTrue();
    assertThat(config.getCpuTriggerConfiguration()).contains("--cpu-threshold 80");
    // File should be deleted after successful read
    assertThat(service.isLocalConfigPresent()).isFalse();
  }

  @Test
  void returnsEmptyOnSecondCallBecauseFileDeletedAfterRead() throws IOException {
    writeConfigFile(validConfigJson());
    LocalProfilerConfigService service = new LocalProfilerConfigService(tempDir);

    // First call returns config and deletes file
    assertThat(service.pullSettings().blockOptional()).isPresent();
    assertThat(service.isLocalConfigPresent()).isFalse();

    // Second call returns empty (file was deleted)
    assertThat(service.pullSettings().blockOptional()).isEmpty();
  }

  @Test
  void returnsConfigWhenNewFileDroppedAfterPreviousConsumed() throws IOException {
    writeConfigFile(validConfigJson());
    LocalProfilerConfigService service = new LocalProfilerConfigService(tempDir);

    // First call consumes and deletes the file
    ProfilerConfiguration config1 = service.pullSettings().block();
    assertThat(config1).isNotNull();
    assertThat(config1.getCpuTriggerConfiguration()).contains("--cpu-threshold 80");
    assertThat(service.isLocalConfigPresent()).isFalse();

    // Drop a new file with different config
    writeConfigFile(validConfigJson().replace("80", "90"));

    // New file is read and deleted
    ProfilerConfiguration config2 = service.pullSettings().block();
    assertThat(config2).isNotNull();
    assertThat(config2.getCpuTriggerConfiguration()).contains("--cpu-threshold 90");
    assertThat(service.isLocalConfigPresent()).isFalse();
  }

  @Test
  void returnsErrorWhenFileMalformed() throws IOException {
    writeConfigFile("this is not json");
    LocalProfilerConfigService service = new LocalProfilerConfigService(tempDir);

    Mono<ProfilerConfiguration> result = service.pullSettings();

    // Malformed file produces an error signal
    assertThatThrownBy(result::block).isNotNull();
  }

  @Test
  void defaultsLastModifiedFromFileTimestamp() throws IOException {
    // Config without lastModified field
    String json =
        "{"
            + "\"enabled\": true,"
            + "\"cpuTriggerConfiguration\": \"--cpu-threshold 80 --cpu-trigger-enabled true\","
            + "\"memoryTriggerConfiguration\": \"--memory-threshold 80 --memory-trigger-enabled true\","
            + "\"collectionPlan\": \"\","
            + "\"requestTriggerConfiguration\": []"
            + "}";
    writeConfigFile(json);
    LocalProfilerConfigService service = new LocalProfilerConfigService(tempDir);

    ProfilerConfiguration config = service.pullSettings().block();

    assertThat(config).isNotNull();
    assertThat(config.getLastModified()).isNotNull();
    assertThat(config.getLastModified().getTime()).isGreaterThan(0);
    // File should be deleted after successful read
    assertThat(service.isLocalConfigPresent()).isFalse();
  }

  @Test
  void doesNotDeleteFileWhenParsingFails() throws IOException {
    writeConfigFile("this is not valid json");
    LocalProfilerConfigService service = new LocalProfilerConfigService(tempDir);

    // Parsing fails
    Mono<ProfilerConfiguration> result = service.pullSettings();
    assertThatThrownBy(result::block).isNotNull();

    // File should still exist so user can fix it
    assertThat(service.isLocalConfigPresent()).isTrue();
  }

  private void writeConfigFile(String content) throws IOException {
    File configFile = new File(tempDir, LocalProfilerConfigService.CONFIG_FILE_NAME);
    try (Writer writer =
        new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
      writer.write(content);
    }
  }

  private static String validConfigJson() {
    return "{"
        + "\"id\": \"local-config\","
        + "\"lastModified\": \"2024-01-01T00:00:00+00:00\","
        + "\"enabledLastModified\": \"2024-01-01T00:00:00+00:00\","
        + "\"enabled\": true,"
        + "\"collectionPlan\": \"--single --mode immediate --immediate-profiling-duration 120 --expiration 5249691022697135638 --settings-moniker local-test\","
        + "\"cpuTriggerConfiguration\": \"--cpu-threshold 80 --cpu-trigger-profilingDuration 120 --cpu-trigger-cooldown 14400 --cpu-trigger-enabled true\","
        + "\"memoryTriggerConfiguration\": \"--memory-threshold 80 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400 --memory-trigger-enabled true\","
        + "\"defaultConfiguration\": null,"
        + "\"requestTriggerConfiguration\": []"
        + "}";
  }
}
