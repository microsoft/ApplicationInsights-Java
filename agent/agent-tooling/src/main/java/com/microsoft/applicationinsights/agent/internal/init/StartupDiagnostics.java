// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.PidFinder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;

class StartupDiagnostics {

  // For Java >= 9
  // Execute with -XX:+UnlockDiagnosticVMOptions -XX:CompilerDirectivesFile=path/jvm-disable-c2.json
  // Content of jvm-disable-c2.json file:
  // [
  //  {
  //    match: [
  //      "*::*"
  //    ],
  //    c2: {
  //      Exclude: true
  //    }
  //  }
  // ]
  private static final String APPPLICATIONINSIGHTS_DEBUG_JIT_C_2_DISABLING_ENABLED =
      "appplicationinsights.debug.jit.c2-disabling.enabled";

  private static final String APPPLICATIONINSIGHTS_DEBUG_JVM_FLAGS_ENABLED =
      "appplicationinsights.debug.jvm-flags.enabled";

  // Execute with -XX:NativeMemoryTracking=summary
  private static final String APPPLICATIONINSIGHTS_DEBUG_NATIVE_MEM_TRACKING_ENABLED =
      "appplicationinsights.debug.native-mem-tracking.enabled";

  // "file" (default value) / "console" / "file-console"
  public static final String APPPLICATIONINSIGHTS_DEBUG_DIAG_EXPORT =
      "appplicationinsights.debug.diag-export";
  private final Logger startupLogger;

  public StartupDiagnostics(Logger startupLogger) {
    this.startupLogger = startupLogger;
  }

  static class DiagnosticsReport {

    StringBuilder diagnosticBuilder = new StringBuilder();

    void addDiagnostic(String diagnostic) {
      diagnosticBuilder.append(diagnostic);
      diagnosticBuilder.append(System.lineSeparator());
      diagnosticBuilder.append(System.lineSeparator());
    }

    boolean isEmpty() {
      return diagnosticBuilder.length() == 0;
    }

    @Override
    public String toString() {
      return diagnosticBuilder.toString();
    }
  }

  void execute() {

    String pid = new PidFinder().getValue();

    executeDiagsAndGenerateReport(pid);

    if (Boolean.getBoolean(APPPLICATIONINSIGHTS_DEBUG_JIT_C_2_DISABLING_ENABLED)) {
      disableC2CompilerDirectives(pid);
    }
  }

  private void executeDiagsAndGenerateReport(String pid) {
    DiagnosticsReport diagnosticsReport = new DiagnosticsReport();

    if (Boolean.getBoolean("rss")) {
      String os = System.getProperty("os.name");
      if (os.equals("Linux")) {
        String residentSetSize = findResidentSetSize();
        diagnosticsReport.addDiagnostic(residentSetSize);
      }
    }

    if (Boolean.getBoolean(APPPLICATIONINSIGHTS_DEBUG_JVM_FLAGS_ENABLED)) {
      String jvmFlags = executeJvmFlagsDiag(pid);
      diagnosticsReport.addDiagnostic(jvmFlags);
    }

    if (Boolean.getBoolean(APPPLICATIONINSIGHTS_DEBUG_NATIVE_MEM_TRACKING_ENABLED)) {
      String nativeSummary = executeNativeMemoryDiag(pid);
      diagnosticsReport.addDiagnostic(nativeSummary);
    }

    generateReport(diagnosticsReport);
  }

  private void generateReport(DiagnosticsReport diagnosticsReport) {
    if (!diagnosticsReport.isEmpty()) {
      String diagExport = System.getProperty(APPPLICATIONINSIGHTS_DEBUG_DIAG_EXPORT);
      if ("console".equals(diagExport) || "file-console".equals(diagExport)) {
        startupLogger.info("Start-up diagnostics" + File.separator + diagnosticsReport);
      }
      // "file" (default value) / "console" / "file-console"
      if (diagExport == null || "file".equals(diagExport) || "file-console".equals(diagExport)) {
        saveIntoFile(diagnosticsReport);
      }
    }
  }

  private String findResidentSetSize() {
    try (Stream<String> lines = Files.lines(Paths.get("/proc/self/status"))) {
      Optional<String> optionalRss = lines.filter(line -> line.startsWith("VmRSS")).findAny();
      return optionalRss.orElse("");
    } catch (IOException e) {
      startupLogger.error("Error when retrieving rss", e);
      return e.getMessage();
    }
  }

  private String executeJvmFlagsDiag(String pid) {
    String[] command = {"jcmd", pid, "VM.flags"};
    return CommandExecutor.executeWithoutException(command, startupLogger);
  }

  private String executeNativeMemoryDiag(String pid) {
    String[] command = {"jcmd", pid, "VM.native_memory", "summary"};
    return CommandExecutor.executeWithoutException(command, startupLogger);
  }

  private void saveIntoFile(DiagnosticsReport diagnosticsReport) {
    Optional<File> optionalTempDir = createTempDirIfNotExists();
    if (optionalTempDir.isPresent()) {
      File tempDir = optionalTempDir.get();
      File diagFile = new File(tempDir, "diagnostics.txt");
      write(diagnosticsReport, diagFile);
    }
  }

  private void write(DiagnosticsReport diagnosticsReport, File diagFile) {
    byte[] diagReportAsBytes = diagnosticsReport.toString().getBytes(StandardCharsets.UTF_8);
    try {
      Files.write(diagFile.toPath(), diagReportAsBytes);
    } catch (IOException e) {
      startupLogger.error("Error occurred when writing diag report.", e);
    }
  }

  private Optional<File> createTempDirIfNotExists() {
    String tempDirectory = System.getProperty("java.io.tmpdir");
    File folder = new File(tempDirectory, "applicationinsights");
    if (!folder.exists() && !folder.mkdirs()) {
      startupLogger.error("Failed to create directory: " + tempDirectory);
      return Optional.empty();
    }
    return Optional.of(folder);
  }

  private static void disableC2CompilerDirectives(String pid) {
    String[] command = {"jcmd", pid, "Compiler.directives_clear"};
    CommandExecutor.execute(command);
  }
}
