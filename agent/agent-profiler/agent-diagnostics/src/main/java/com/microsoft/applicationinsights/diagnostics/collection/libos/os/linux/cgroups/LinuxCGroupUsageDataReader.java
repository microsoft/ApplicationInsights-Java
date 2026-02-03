// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupUsageDataReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LinuxCGroupUsageDataReader implements CGroupUsageDataReader {
  private static final Path CGROUP_CPU_PATH = Paths.get("./cpu,cpuacct/");

  private final CGroupCpuUsageReader cgroupCpuUsageReader;

  private final CGroupCpuUserReader cgroupCpuUserReader;

  private final CGroupCpuSystemReader cgroupCpuSystemReader;

  private final CGroupStatReader cgroupStatReader;

  private final Path cgroupDirectory;

  public LinuxCGroupUsageDataReader(Path cgroupDirectory) {
    this.cgroupDirectory = cgroupDirectory.resolve(CGROUP_CPU_PATH);
    cgroupCpuUsageReader = new CGroupCpuUsageReader(this.cgroupDirectory);
    cgroupCpuUserReader = new CGroupCpuUserReader(this.cgroupDirectory);
    cgroupCpuSystemReader = new CGroupCpuSystemReader(this.cgroupDirectory);
    cgroupStatReader = new CGroupStatReader(this.cgroupDirectory);
  }

  @Override
  public void poll() {
    cgroupCpuSystemReader.poll();
    cgroupCpuUsageReader.poll();
    cgroupCpuUserReader.poll();
    cgroupStatReader.poll();
  }

  @Override
  public void update() {
    cgroupCpuSystemReader.update();
    cgroupCpuUsageReader.update();
    cgroupCpuUserReader.update();
    cgroupStatReader.update();
  }

  @Override
  public List<Double> getTelemetry() {
    return Stream.of(
            cgroupCpuUsageReader.getUsage().getIncrement(),
            cgroupCpuUserReader.getUsage().getIncrement(),
            cgroupCpuSystemReader.getUsage().getIncrement(),
            cgroupStatReader.getUser().getIncrement(),
            cgroupStatReader.getSystem().getIncrement())
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
    return Files.exists(cgroupDirectory);
  }

  @Override
  public void close() throws IOException {
    cgroupCpuUsageReader.close();
    cgroupCpuUserReader.close();
    cgroupCpuSystemReader.close();
    cgroupStatReader.close();
  }
}
