// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups;

import static com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups.CGroupv2CpuReaderTest.writeLines;
import static com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups.CGroupv2CpuReaderTest.writeString;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroupsv2.LinuxCGroupV2UsageDataReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link LinuxCGroupV2UsageDataReader} verifying cgroup v2 usage data reading with
 * arbitrary mount locations.
 */
@EnabledOnOs(OS.LINUX)
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class LinuxCGroupV2UsageDataReaderTest {

  @TempDir Path tempDir;

  private Path cgroupDir;

  @BeforeEach
  void setUp() throws IOException {
    cgroupDir = tempDir.resolve("cgroup2");
    Files.createDirectories(cgroupDir);
  }

  @Test
  void isAvailableReturnsTrueWhenCgroupControllersExists() throws Exception {
    // Arrange - cgroup.controllers is the indicator file for cgroup v2
    writeString(cgroupDir.resolve("cgroup.controllers"), "cpu memory io");

    LinuxCGroupV2UsageDataReader reader = new LinuxCGroupV2UsageDataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.isAvailable()).isTrue();
  }

  @Test
  void isAvailableReturnsFalseWhenCgroupControllersDoesNotExist() {
    // Arrange - cgroupDir exists but no cgroup.controllers file
    LinuxCGroupV2UsageDataReader reader = new LinuxCGroupV2UsageDataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.isAvailable()).isFalse();
  }

  @Test
  void parsesCpuStatFileCorrectly() throws Exception {
    // Arrange
    createCgroupV2CpuStatFile(cgroupDir, 1000000, 600000, 400000);
    writeString(cgroupDir.resolve("cgroup.controllers"), "cpu memory io");

    LinuxCGroupV2UsageDataReader reader = new LinuxCGroupV2UsageDataReader(cgroupDir);

    // First poll/update
    reader.poll();
    reader.update();

    // Update with new values
    createCgroupV2CpuStatFile(cgroupDir, 3000000, 1800000, 1200000);
    reader.poll();
    reader.update();

    // Act
    List<Double> telemetry = reader.getTelemetry();

    // Assert
    assertThat(telemetry).hasSize(5);
    // The increments should be: usage=2000000, user=1200000, system=800000
    assertThat(telemetry.get(0)).isEqualTo(2000000.0d); // usage increment
    assertThat(telemetry.get(1)).isEqualTo(1200000.0d); // user increment
    assertThat(telemetry.get(2)).isEqualTo(800000.0d); // system increment
  }

  @Test
  void worksWithCustomMountLocation() throws Exception {
    // Arrange - simulate a custom cgroup v2 mount like /sys/fs/cgroup/user.slice/user-1000.slice
    Path customMount = tempDir.resolve("sys").resolve("fs").resolve("cgroup").resolve("user.slice");
    Files.createDirectories(customMount);

    createCgroupV2CpuStatFile(customMount, 5000000, 2500000, 2500000);
    writeString(customMount.resolve("cgroup.controllers"), "cpu memory");

    LinuxCGroupV2UsageDataReader reader = new LinuxCGroupV2UsageDataReader(customMount);

    // Act & Assert
    assertThat(reader.isAvailable()).isTrue();

    reader.poll();
    reader.update();

    List<Double> telemetry = reader.getTelemetry();
    assertThat(telemetry).hasSize(5);
  }

  @Test
  void closesResourcesProperly() throws Exception {
    // Arrange
    createCgroupV2CpuStatFile(cgroupDir, 1000000, 500000, 500000);
    writeString(cgroupDir.resolve("cgroup.controllers"), "cpu memory io");

    LinuxCGroupV2UsageDataReader reader = new LinuxCGroupV2UsageDataReader(cgroupDir);

    reader.poll();
    reader.update();

    // Act & Assert - should not throw
    reader.close();
  }

  @Test
  void returnsNegativeOneForFirstPoll() throws Exception {
    // Arrange
    createCgroupV2CpuStatFile(cgroupDir, 1000000, 500000, 500000);
    writeString(cgroupDir.resolve("cgroup.controllers"), "cpu memory io");

    LinuxCGroupV2UsageDataReader reader = new LinuxCGroupV2UsageDataReader(cgroupDir);

    // Act - only first poll/update (no increment available yet)
    reader.poll();
    reader.update();
    List<Double> telemetry = reader.getTelemetry();

    // Assert - first read has no increment, so values should be -1.0
    assertThat(telemetry).hasSize(5);
    assertThat(telemetry).allMatch(value -> value == -1.0d);
  }

  private static void createCgroupV2CpuStatFile(Path dir, long usage, long user, long system)
      throws IOException {
    writeLines(
        dir.resolve("cpu.stat"),
        "usage_usec " + usage,
        "user_usec " + user,
        "system_usec " + system);
  }
}
