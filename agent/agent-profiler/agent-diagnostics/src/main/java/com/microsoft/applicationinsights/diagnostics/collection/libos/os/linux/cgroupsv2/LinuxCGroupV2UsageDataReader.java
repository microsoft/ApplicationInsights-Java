// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroupsv2;

import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupUsageDataReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LinuxCGroupV2UsageDataReader implements CGroupUsageDataReader {

  private final CGroupv2CpuReader cgroupV2CpuReader;
  private final Path cgroupDir;

  public LinuxCGroupV2UsageDataReader(Path cgroupDir) {
    this.cgroupDir = cgroupDir;
    cgroupV2CpuReader = new CGroupv2CpuReader(cgroupDir);
  }

  @Override
  public void poll() {
    cgroupV2CpuReader.poll();
  }

  @Override
  public void update() {
    cgroupV2CpuReader.update();
  }

  @Override
  public List<Double> getTelemetry() {
    return Stream.of(
            cgroupV2CpuReader.getCpuUsage().getIncrement(),
            cgroupV2CpuReader.getCpuUser().getIncrement(),
            cgroupV2CpuReader.getCpuSystem().getIncrement(),
            cgroupV2CpuReader.getCpuUser().getIncrement(),
            cgroupV2CpuReader.getCpuSystem().getIncrement())
        .map(
            value -> {
              if (value == null) {
                return -1.0d;
              } else {
                return value.doubleValue();
              }
            })
        .collect(Collectors.toList());
  }

  @Override
  public boolean isAvailable() {
    return Files.exists(cgroupDir.resolve("./cgroup.controllers"));
  }

  @Override
  public void close() throws IOException {
    cgroupV2CpuReader.close();
  }
}
