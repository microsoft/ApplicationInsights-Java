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

  @SuppressWarnings("checkstyle:MemberName")
  private final CGroupCPUUsageReader cGroupCpuUsageReader = new CGroupCPUUsageReader();

  @SuppressWarnings("checkstyle:MemberName")
  private final CGroupCPUUserReader cGroupCpuUserReader = new CGroupCPUUserReader();

  @SuppressWarnings("checkstyle:MemberName")
  private final CGroupCPUSystemReader cGroupCpuSystemReader = new CGroupCPUSystemReader();

  @SuppressWarnings("checkstyle:MemberName")
  private final CGroupStatReader cGroupStatReader = new CGroupStatReader();

  @Override
  public void poll() {
    cGroupCpuSystemReader.poll();
    cGroupCpuUsageReader.poll();
    cGroupCpuUserReader.poll();
    cGroupStatReader.poll();
  }

  @Override
  public void update() {
    cGroupCpuSystemReader.update();
    cGroupCpuUsageReader.update();
    cGroupCpuUserReader.update();
    cGroupStatReader.update();
  }

  @Override
  public List<Double> getTelemetry() {
    return Stream.of(
            cGroupCpuUsageReader.getUsage().getIncrement(),
            cGroupCpuUserReader.getUsage().getIncrement(),
            cGroupCpuSystemReader.getUsage().getIncrement(),
            cGroupStatReader.getUser().getIncrement(),
            cGroupStatReader.getSystem().getIncrement())
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
    cGroupCpuUsageReader.close();
    cGroupCpuUserReader.close();
    cGroupCpuSystemReader.close();
    cGroupStatReader.close();
  }
}
