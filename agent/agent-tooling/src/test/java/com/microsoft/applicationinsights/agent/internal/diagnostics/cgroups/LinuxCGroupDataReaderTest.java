// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups;

import static com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups.CGroupv2CpuReaderTest.writeString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups.LinuxCGroupDataReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link LinuxCGroupDataReader} verifying cgroup v1 data reading with arbitrary mount
 * locations.
 */
@EnabledOnOs(OS.LINUX)
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class LinuxCGroupDataReaderTest {

  @TempDir Path tempDir;

  private Path cgroupRoot;

  @BeforeEach
  void setUp() throws IOException {
    cgroupRoot = tempDir.resolve("cgroup");
    Files.createDirectories(cgroupRoot);
  }

  @Test
  void readsMemoryLimitsFromCustomMountLocation() throws Exception {
    // Arrange
    Path memoryDir = cgroupRoot.resolve("memory");
    Files.createDirectories(memoryDir);
    writeString(memoryDir.resolve("memory.limit_in_bytes"), "1073741824");
    writeString(memoryDir.resolve("memory.soft_limit_in_bytes"), "536870912");
    writeString(memoryDir.resolve("memory.kmem.limit_in_bytes"), "268435456");

    LinuxCGroupDataReader reader = new LinuxCGroupDataReader(cgroupRoot);

    // Act & Assert
    assertThat(reader.getMemoryLimit()).isEqualTo(1073741824L);
    assertThat(reader.getMemorySoftLimit()).isEqualTo(536870912L);
    assertThat(reader.getKmemLimit()).isEqualTo(268435456L);
  }

  @Test
  void readsCpuLimitsFromCustomMountLocation() throws Exception {
    // Arrange
    Path cpuDir = cgroupRoot.resolve("cpu,cpuacct");
    Files.createDirectories(cpuDir);
    writeString(cpuDir.resolve("cpu.cfs_quota_us"), "50000");
    writeString(cpuDir.resolve("cpu.cfs_period_us"), "100000");

    LinuxCGroupDataReader reader = new LinuxCGroupDataReader(cgroupRoot);

    // Act & Assert
    assertThat(reader.isAvailable()).isTrue();
    assertThat(reader.getCpuLimit()).isEqualTo(50000L);
    assertThat(reader.getCpuPeriod()).isEqualTo(100000L);
  }

  @Test
  void isAvailableReturnsFalseWhenNoFilesExist() {
    // Arrange - cgroupRoot exists but has no cgroup files
    LinuxCGroupDataReader reader = new LinuxCGroupDataReader(cgroupRoot);

    // Act & Assert
    assertThat(reader.isAvailable()).isFalse();
  }

  @Test
  void throwsExceptionWhenFileDoesNotExist() {
    // Arrange
    LinuxCGroupDataReader reader = new LinuxCGroupDataReader(cgroupRoot);

    // Act & Assert
    assertThatThrownBy(reader::getMemoryLimit)
        .isInstanceOf(OperatingSystemInteractionException.class);
  }

  @Test
  void worksWithDeeplyNestedCustomMountLocation() throws Exception {
    // Arrange - simulate a deeply nested custom mount point
    Path deepPath = tempDir.resolve("custom").resolve("mount").resolve("point").resolve("cgroup");
    Files.createDirectories(deepPath);

    Path memoryDir = deepPath.resolve("memory");
    Files.createDirectories(memoryDir);
    writeString(memoryDir.resolve("memory.limit_in_bytes"), "2147483648");

    Path cpuDir = deepPath.resolve("cpu,cpuacct");
    Files.createDirectories(cpuDir);
    writeString(cpuDir.resolve("cpu.cfs_quota_us"), "100000");
    writeString(cpuDir.resolve("cpu.cfs_period_us"), "100000");

    LinuxCGroupDataReader reader = new LinuxCGroupDataReader(deepPath);

    // Act & Assert
    assertThat(reader.isAvailable()).isTrue();
    assertThat(reader.getMemoryLimit()).isEqualTo(2147483648L);
    assertThat(reader.getCpuLimit()).isEqualTo(100000L);
    assertThat(reader.getCpuPeriod()).isEqualTo(100000L);
  }
}
