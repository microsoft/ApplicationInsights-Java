// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups;

import static com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups.CGroupv2CpuReaderTest.writeLines;
import static com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups.CGroupv2CpuReaderTest.writeString;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups.LinuxCGroupUsageDataReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link LinuxCGroupUsageDataReader} verifying cgroup v1 usage data reading with
 * arbitrary mount locations.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class LinuxCGroupUsageDataReaderTest {

  @TempDir Path tempDir;

  private Path cgroupRoot;
  private Path cpuCgroupDir;

  @BeforeEach
  void setUp() throws IOException {
    cgroupRoot = tempDir.resolve("cgroup");
    cpuCgroupDir = cgroupRoot.resolve("cpu,cpuacct");
    Files.createDirectories(cpuCgroupDir);
  }

  @Test
  void isAvailableReturnsTrueWhenCpuDirectoryExists() throws Exception {
    // Arrange
    LinuxCGroupUsageDataReader reader = new LinuxCGroupUsageDataReader(cgroupRoot);

    // Act & Assert
    assertThat(reader.isAvailable()).isTrue();
  }

  @Test
  void isAvailableReturnsFalseWhenCpuDirectoryDoesNotExist() throws Exception {
    // Arrange - use a path without the cpu,cpuacct directory
    Path emptyRoot = tempDir.resolve("empty");
    Files.createDirectories(emptyRoot);

    LinuxCGroupUsageDataReader reader = new LinuxCGroupUsageDataReader(emptyRoot);

    // Act & Assert
    assertThat(reader.isAvailable()).isFalse();
  }

  @Test
  void worksWithCustomMountLocation() throws Exception {
    // Arrange - simulate a custom mount location like /custom/cgroup
    Path customMount = tempDir.resolve("custom").resolve("cgroup");
    Path customCpuDir = customMount.resolve("cpu,cpuacct");
    Files.createDirectories(customCpuDir);

    createCgroupV1CpuFiles(customCpuDir, "5000000", "2500000", "2500000", "50", "50");

    LinuxCGroupUsageDataReader reader = new LinuxCGroupUsageDataReader(customMount);

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
    createCgroupV1CpuFiles(cpuCgroupDir, "1000000", "500000", "500000", "100", "100");
    LinuxCGroupUsageDataReader reader = new LinuxCGroupUsageDataReader(cgroupRoot);

    reader.poll();
    reader.update();

    // Act & Assert - should not throw
    reader.close();
  }

  @Test
  void returnsNegativeOneForUnreadableValues() throws Exception {
    // Arrange - create directory but no files
    LinuxCGroupUsageDataReader reader = new LinuxCGroupUsageDataReader(cgroupRoot);

    // Act
    reader.poll();
    reader.update();
    List<Double> telemetry = reader.getTelemetry();

    // Assert - all values should be -1.0 since files don't exist
    assertThat(telemetry).hasSize(5);
    assertThat(telemetry).allMatch(value -> value == -1.0d);
  }

  private static void createCgroupV1CpuFiles(
      Path cpuDir,
      String usage,
      String userUsage,
      String systemUsage,
      String statUser,
      String statSystem)
      throws IOException {
    writeString(cpuDir.resolve("cpuacct.usage"), usage);
    writeString(cpuDir.resolve("cpuacct.usage_user"), userUsage);
    writeString(cpuDir.resolve("cpuacct.usage_sys"), systemUsage);
    writeLines(cpuDir.resolve("cpuacct.stat"), "user " + statUser, "system " + statSystem);
  }
}
