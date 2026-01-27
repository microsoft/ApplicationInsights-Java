// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups;

import static com.microsoft.applicationinsights.agent.internal.diagnostics.cgroups.CGroupv2CpuReaderTest.writeString;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroupsv2.LinuxCGroupV2DataReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link LinuxCGroupV2DataReader} verifying cgroup v2 data reading with arbitrary mount
 * locations.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class LinuxCGroupV2DataReaderTest {

  @TempDir Path tempDir;

  private Path cgroupDir;

  @BeforeEach
  void setUp() throws IOException {
    cgroupDir = tempDir.resolve("cgroup2");
    Files.createDirectories(cgroupDir);
  }

  @Test
  void readsMemoryMaxFromCustomMountLocation() throws Exception {
    // Arrange
    writeString(cgroupDir.resolve("memory.max"), "1073741824");
    writeString(cgroupDir.resolve("memory.high"), "536870912");
    writeString(cgroupDir.resolve("cpu.max"), "50000 100000");

    LinuxCGroupV2DataReader reader = new LinuxCGroupV2DataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.getMemoryLimit()).isEqualTo(1073741824L);
  }

  @Test
  void readsMemoryHighAsSoftLimit() throws Exception {
    // Arrange
    writeString(cgroupDir.resolve("memory.max"), "1073741824");
    writeString(cgroupDir.resolve("memory.high"), "536870912");
    writeString(cgroupDir.resolve("cpu.max"), "50000 100000");

    LinuxCGroupV2DataReader reader = new LinuxCGroupV2DataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.getMemorySoftLimit()).isEqualTo(536870912L);
  }

  @Test
  void parsesMaxValueAsLongMaxValue() throws Exception {
    // Arrange - "max" means no limit in cgroup v2
    writeString(cgroupDir.resolve("memory.max"), "max");
    writeString(cgroupDir.resolve("memory.high"), "max");
    writeString(cgroupDir.resolve("cpu.max"), "max 100000");

    LinuxCGroupV2DataReader reader = new LinuxCGroupV2DataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.getMemoryLimit()).isEqualTo(Long.MAX_VALUE);
    assertThat(reader.getMemorySoftLimit()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  void readsCpuQuotaFromCpuMax() throws Exception {
    // Arrange - cpu.max format is "quota period"
    writeString(cgroupDir.resolve("memory.max"), "1073741824");
    writeString(cgroupDir.resolve("memory.high"), "536870912");
    writeString(cgroupDir.resolve("cpu.max"), "50000 100000");

    LinuxCGroupV2DataReader reader = new LinuxCGroupV2DataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.getCpuLimit()).isEqualTo(50000L);
  }

  @Test
  void readsCpuPeriodFromCpuMax() throws Exception {
    // Arrange - cpu.max format is "quota period"
    writeString(cgroupDir.resolve("memory.max"), "1073741824");
    writeString(cgroupDir.resolve("memory.high"), "536870912");
    writeString(cgroupDir.resolve("cpu.max"), "50000 100000");

    LinuxCGroupV2DataReader reader = new LinuxCGroupV2DataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.getCpuPeriod()).isEqualTo(100000L);
  }

  @Test
  void cpuQuotaReturnsNegativeOneWhenMax() throws Exception {
    // Arrange - "max" for cpu quota means unlimited
    writeString(cgroupDir.resolve("memory.max"), "1073741824");
    writeString(cgroupDir.resolve("memory.high"), "536870912");
    writeString(cgroupDir.resolve("cpu.max"), "max 100000");

    LinuxCGroupV2DataReader reader = new LinuxCGroupV2DataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.getCpuLimit()).isEqualTo(-1L);
  }

  @Test
  void kmemLimitReturnsMaxValueForCgroupV2() throws Exception {
    // Arrange - cgroup v2 doesn't separately expose kernel memory
    writeString(cgroupDir.resolve("memory.max"), "1073741824");
    writeString(cgroupDir.resolve("memory.high"), "536870912");
    writeString(cgroupDir.resolve("cpu.max"), "50000 100000");

    LinuxCGroupV2DataReader reader = new LinuxCGroupV2DataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.getKmemLimit()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  void isAvailableReturnsTrueWhenAtLeastOneFileExists() throws Exception {
    // Arrange
    writeString(cgroupDir.resolve("memory.max"), "1073741824");

    LinuxCGroupV2DataReader reader = new LinuxCGroupV2DataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.isAvailable()).isTrue();
  }

  @Test
  void isAvailableReturnsFalseWhenNoFilesExist() {
    // Arrange - cgroupDir exists but has no cgroup v2 files
    LinuxCGroupV2DataReader reader = new LinuxCGroupV2DataReader(cgroupDir);

    // Act & Assert
    assertThat(reader.isAvailable()).isFalse();
  }

  @Test
  void worksWithDeeplyNestedCustomMountLocation() throws Exception {
    // Arrange - simulate a deeply nested custom mount point
    Path deepPath = tempDir.resolve("sys").resolve("fs").resolve("cgroup").resolve("user.slice");
    Files.createDirectories(deepPath);
    writeString(deepPath.resolve("memory.max"), "2147483648");
    writeString(deepPath.resolve("memory.high"), "1073741824");
    writeString(deepPath.resolve("cpu.max"), "200000 100000");

    LinuxCGroupV2DataReader reader = new LinuxCGroupV2DataReader(deepPath);

    // Act & Assert
    assertThat(reader.isAvailable()).isTrue();
    assertThat(reader.getMemoryLimit()).isEqualTo(2147483648L);
    assertThat(reader.getMemorySoftLimit()).isEqualTo(1073741824L);
    assertThat(reader.getCpuLimit()).isEqualTo(200000L);
    assertThat(reader.getCpuPeriod()).isEqualTo(100000L);
  }
}
