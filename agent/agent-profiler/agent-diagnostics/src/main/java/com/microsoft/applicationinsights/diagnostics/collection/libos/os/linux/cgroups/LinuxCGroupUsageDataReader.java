// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupUsageDataReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LinuxCGroupUsageDataReader implements CGroupUsageDataReader {

  private final CGroupCpuUsageReader cgroupCpuUsageReader = new CGroupCpuUsageReader();

  private final CGroupCpuUserReader cgroupCpuUserReader = new CGroupCpuUserReader();

  private final CGroupCpuSystemReader cgroupCpuSystemReader = new CGroupCpuSystemReader();

  private final CGroupStatReader cgroupStatReader = new CGroupStatReader();

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
  public void close() throws IOException {
    cgroupCpuUsageReader.close();
    cgroupCpuUserReader.close();
    cgroupCpuSystemReader.close();
    cgroupStatReader.close();
  }
}
