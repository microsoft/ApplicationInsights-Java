// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import com.microsoft.applicationinsights.diagnostics.collection.libos.process.Process;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessCPUStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessIOStats;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents processes, also provides the ability to gather per-process stats, such as cpu and io
 * usage.
 */
@SuppressFBWarnings(value = "Eq")
public class LinuxProcess extends Process implements Closeable {

  private static final List<String> JAVA_ARGUMENTS =
      Arrays.asList(
          "-d32",
          "-d64",
          "-server",
          "-D",
          "-verbose",
          "-version",
          "-showversion",
          "-jre-restrict-search",
          "-jre-no-restrict-search",
          "-?",
          "-help",
          "-X",
          "-XX",
          "-ea",
          "-enableassertions",
          "-da",
          "-disableassertions",
          "-esa",
          "-enablesystemassertions",
          "-dsa",
          "-disablesystemassertions",
          "-agentlib",
          "-agentpath",
          "-javaagent");

  private static final List<String> JAVA_ARGUMENT_WITH_SUFFIX = Arrays.asList("-cp", "-classpath");

  private static final File DEFAULT_HSPERF_DIR =
      new File("/tmp/hsperfdata_" + System.getProperty("user.name"));

  private final LinuxProcessIOStats ioStats;
  private final LinuxProcessCPUStats cpuStats;

  public LinuxProcess(int pid, File candidate) throws IOException {
    this(pid, DEFAULT_HSPERF_DIR, parseFullName(Proc.TOP_DIR, pid), candidate);
  }

  public LinuxProcess(int pid, File hsperfDir, String fullName, File candidate) {
    super(parseName(fullName, checkJava(hsperfDir, pid), pid), pid);
    this.isJava = checkJava(hsperfDir, pid);
    this.ioStats = new LinuxProcessIOStats(candidate);
    this.cpuStats = new LinuxProcessCPUStats(candidate);
  }

  public static LinuxProcess create(File procDir, File hsperfDir, int pid, File candidate)
      throws IOException {
    String fullName = parseFullName(procDir, pid);
    return new LinuxProcess(pid, hsperfDir, fullName, candidate);
  }

  protected static String parseName(String fullName, boolean isJava, int pid) {
    String[] args = fullName.split(String.valueOf((char) 0));

    if (args.length == 0) {
      return fullName.replace('\n', ' ');
    }

    if (!isJava) {
      return args[0].replace('\n', ' ');
    }

    String mainClass = getJavaMainClass(args);
    if (mainClass == null) {
      return "java pid: " + pid;
    }

    return "java " + mainClass;
  }

  @Nullable
  protected static String getJavaMainClass(String[] args) {
    for (int i = 1; i < args.length; i++) {
      if (!isJavaArgument(i, args)) {
        return args[i];
      }
    }
    return null;
  }

  protected static boolean isJavaArgument(int index, String... args) {
    return hasPrefixIn(args[index], JAVA_ARGUMENTS)
        || hasPrefixIn(args[index], JAVA_ARGUMENT_WITH_SUFFIX)
        || (index > 0 && hasPrefixIn(args[index - 1], JAVA_ARGUMENT_WITH_SUFFIX));
  }

  private static boolean hasPrefixIn(String full, List<String> prefixes) {
    for (String prefix : prefixes) {
      if (full.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  @SuppressFBWarnings(
      value = "SECPTI" // Potential Path Traversal
      )
  protected static String parseFullName(File procDir, int pid) throws IOException {
    return Proc.read(new File(procDir + "/" + pid, "cmdline"));
  }

  protected static boolean checkJava(File hsperfDir, int pid) {
    return new File(hsperfDir, String.valueOf(pid)).exists();
  }

  @Override
  public void close() throws IOException {
    cpuStats.close();
    ioStats.close();
  }

  @Override
  public ProcessIOStats getIoStats() {
    return ioStats;
  }

  @Override
  public ProcessCPUStats getCpuStats() {
    return cpuStats;
  }

  @Override
  public void update() {
    ioStats.update();
    cpuStats.update();
  }

  @Override
  public void poll() {
    ioStats.poll();
    cpuStats.poll();
  }
}
