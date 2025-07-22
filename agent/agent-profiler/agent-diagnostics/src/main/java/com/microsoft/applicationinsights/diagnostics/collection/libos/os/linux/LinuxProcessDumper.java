// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import com.microsoft.applicationinsights.diagnostics.collection.libos.process.Process;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessDumper;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Detects running processes on the host. */
public class LinuxProcessDumper implements ProcessDumper, Closeable {
  private static final Logger logger = LoggerFactory.getLogger(LinuxProcessDumper.class);

  private final boolean isDaemon;
  private Map<Integer, LinuxProcess> usage = new HashMap<>();

  private final int thisPid;

  public LinuxProcessDumper(boolean isDaemon, int thisPid) {
    this.thisPid = thisPid;
    this.isDaemon = isDaemon;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Iterable<Process> all(boolean includeSelf) {
    if (includeSelf || !isDaemon) {
      return (Iterable) usage.values();
    } else {
      List<Process> processes = new ArrayList<>(usage.values());
      processes.removeIf(process -> process.getPid() == thisPid);
      return processes;
    }
  }

  @Override
  public void poll() {
    Map<Integer, LinuxProcess> updatedUsage = new HashMap<>();

    for (File candidate : Objects.requireNonNull(Proc.TOP_DIR.listFiles())) {
      if (candidate.isDirectory()) {
        String filename = candidate.getName();
        char firstChar = filename.charAt(0);
        if (Character.isDigit(firstChar)) {
          try {
            int pid = Integer.parseInt(filename);
            LinuxProcess process = recordProcess(candidate, pid);
            updatedUsage.put(pid, process);
          } catch (NumberFormatException | IOException e) {
            // NOP
          }
        }
      }
    }
    usage = updatedUsage;
  }

  @Override
  public void closeProcesses(List<Integer> exclusions) {
    List<Integer> toRemove =
        usage.keySet().stream().filter(it -> !exclusions.contains(it)).collect(Collectors.toList());

    toRemove.forEach(
        exclusion -> {
          LinuxProcess removed = usage.remove(exclusion);
          try {
            removed.close();
          } catch (IOException e) {
            logger.error("Failed to close process", e);
          }
        });
  }

  private LinuxProcess recordProcess(File candidate, int pid) throws IOException {
    LinuxProcess process = usage.get(pid);

    if (process == null) {
      process = LinuxProcess.create(pid, candidate);
    }
    process.poll();
    process.update();
    return process;
  }

  public LinuxProcess getProcess(int pid) {
    return usage.get(pid);
  }

  @Override
  public void close() throws IOException {
    for (LinuxProcess process : usage.values()) {
      process.close();
    }
  }

  @Override
  public Process thisProcess() {
    return getProcess(thisPid);
  }
}
