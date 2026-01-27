// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroupsv2.CGroupv2CpuReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link CGroupv2CpuReader} verifying cgroup v2 CPU stat file parsing. */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class CGroupv2CpuReaderTest {

  @TempDir Path tempDir;

  private Path cgroupDir;

  @BeforeEach
  void setUp() throws IOException {
    cgroupDir = tempDir.resolve("cgroup2");
    Files.createDirectories(cgroupDir);
  }

  public static void writeString(Path path, String content) throws IOException {
    Files.write(path, Collections.singletonList(content), StandardCharsets.UTF_8);
  }

  public static void writeLines(Path path, String... lines) throws IOException {
    Files.write(path, Arrays.asList(lines), StandardCharsets.UTF_8);
  }

  @Test
  void parsesCpuStatFields() throws Exception {
    // Arrange
    createCpuStatFile(1000000, 600000, 400000);
    CGroupv2CpuReader reader = new CGroupv2CpuReader(cgroupDir);

    // Act - first poll/update to set baseline
    reader.poll();
    reader.update();

    // Update file and poll again
    createCpuStatFile(2000000, 1200000, 800000);
    reader.poll();
    reader.update();

    // Assert
    assertThat(reader.getCpuUsage().getIncrement()).isEqualTo(1000000L);
    assertThat(reader.getCpuUser().getIncrement()).isEqualTo(600000L);
    assertThat(reader.getCpuSystem().getIncrement()).isEqualTo(400000L);
  }

  @Test
  void handlesAdditionalFieldsInCpuStat() throws Exception {
    // Arrange - cpu.stat may contain additional fields like nr_periods, nr_throttled
    writeLines(
        cgroupDir.resolve("cpu.stat"),
        "usage_usec 1000000",
        "user_usec 600000",
        "system_usec 400000",
        "nr_periods 100",
        "nr_throttled 5",
        "throttled_usec 50000",
        "nr_bursts 0",
        "burst_usec 0");

    CGroupv2CpuReader reader = new CGroupv2CpuReader(cgroupDir);

    // Act
    reader.poll();
    reader.update();

    // Update with new values
    writeLines(
        cgroupDir.resolve("cpu.stat"),
        "usage_usec 2000000",
        "user_usec 1200000",
        "system_usec 800000",
        "nr_periods 200",
        "nr_throttled 10",
        "throttled_usec 100000",
        "nr_bursts 0",
        "burst_usec 0");
    reader.poll();
    reader.update();

    // Assert - should only parse the usage, user, and system fields
    assertThat(reader.getCpuUsage().getIncrement()).isEqualTo(1000000L);
    assertThat(reader.getCpuUser().getIncrement()).isEqualTo(600000L);
    assertThat(reader.getCpuSystem().getIncrement()).isEqualTo(400000L);
  }

  @Test
  void closesResourcesProperly() throws Exception {
    // Arrange
    createCpuStatFile(1000000, 600000, 400000);
    CGroupv2CpuReader reader = new CGroupv2CpuReader(cgroupDir);

    reader.poll();
    reader.update();

    // Act & Assert - should not throw
    reader.close();
  }

  @Test
  void incrementIsNullOnFirstPoll() throws Exception {
    // Arrange
    createCpuStatFile(1000000, 600000, 400000);
    CGroupv2CpuReader reader = new CGroupv2CpuReader(cgroupDir);

    // Act - only first poll/update
    reader.poll();
    reader.update();

    // Assert - no increment yet since we need two values to calculate
    assertThat(reader.getCpuUsage().getIncrement()).isNull();
    assertThat(reader.getCpuUser().getIncrement()).isNull();
    assertThat(reader.getCpuSystem().getIncrement()).isNull();
  }

  @Test
  void worksWithCustomMountPath() throws Exception {
    // Arrange - deeply nested custom path
    Path customPath = tempDir.resolve("sys").resolve("fs").resolve("cgroup").resolve("app");
    Files.createDirectories(customPath);

    writeLines(
        customPath.resolve("cpu.stat"),
        "usage_usec 5000000",
        "user_usec 3000000",
        "system_usec 2000000");

    CGroupv2CpuReader reader = new CGroupv2CpuReader(customPath);

    // Act
    reader.poll();
    reader.update();

    writeLines(
        customPath.resolve("cpu.stat"),
        "usage_usec 10000000",
        "user_usec 6000000",
        "system_usec 4000000");
    reader.poll();
    reader.update();

    // Assert
    assertThat(reader.getCpuUsage().getIncrement()).isEqualTo(5000000L);
    assertThat(reader.getCpuUser().getIncrement()).isEqualTo(3000000L);
    assertThat(reader.getCpuSystem().getIncrement()).isEqualTo(2000000L);
  }

  private void createCpuStatFile(long usage, long user, long system) throws IOException {
    writeLines(
        cgroupDir.resolve("cpu.stat"),
        "usage_usec " + usage,
        "user_usec " + user,
        "system_usec " + system);
  }
}
