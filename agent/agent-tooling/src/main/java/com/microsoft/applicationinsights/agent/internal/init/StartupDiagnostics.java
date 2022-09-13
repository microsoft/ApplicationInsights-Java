// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.PidFinder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StartupDiagnostics {

  public static final String APPLICATIONINSIGHTS_DEBUG_RSS_ENABLED =
      "applicationinsights.debug.startup.rss.enabled";

  // Execute with -XX:NativeMemoryTracking=summary
  private static final String APPLICATIONINSIGHTS_DEBUG_NATIVE_MEM_TRACKING_ENABLED =
      "applicationinsights.debug.startup.native-mem-tracking.enabled";

  public static final String APPLICATIONINSIGHTS_DEBUG_DIAG_EXPORT_TO_FILE =
      "applicationinsights.debug.startup.diag-export-to-file";

  private static final Logger startupLogger =
      LoggerFactory.getLogger("com.microsoft.applicationinsights.agent");

  static class DiagnosticsReport {

    final StringBuilder diagnosticBuilder = new StringBuilder();

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

  private StartupDiagnostics() {}

  static void execute() {

    DiagnosticsReport diagnosticsReport = new DiagnosticsReport();

    if (Boolean.getBoolean(APPLICATIONINSIGHTS_DEBUG_RSS_ENABLED)) {
      String os = System.getProperty("os.name");
      if (os.equals("Linux")) {
        String residentSetSize = findResidentSetSize();
        diagnosticsReport.addDiagnostic(residentSetSize);
      }
    }

    if (Boolean.getBoolean(APPLICATIONINSIGHTS_DEBUG_NATIVE_MEM_TRACKING_ENABLED)) {
      String nativeSummary = executeNativeMemoryDiag();
      diagnosticsReport.addDiagnostic(nativeSummary);
    }

    generateReport(diagnosticsReport);
  }

  private static void generateReport(DiagnosticsReport diagnosticsReport) {
    if (!diagnosticsReport.isEmpty()) {
      startupLogger.info("Start-up diagnostics" + File.separator + diagnosticsReport);
      boolean exportToFile = Boolean.getBoolean(APPLICATIONINSIGHTS_DEBUG_DIAG_EXPORT_TO_FILE);
      if (exportToFile) {
        saveIntoFile(diagnosticsReport);
      }
    }
  }

  private static String findResidentSetSize() {
    try (Stream<String> lines = Files.lines(Paths.get("/proc/self/status"))) {
      return lines.filter(line -> line.startsWith("VmRSS")).findAny().orElse("");
    } catch (IOException e) {
      startupLogger.error("Error when retrieving rss", e);
      return e.getMessage();
    }
  }

  private static void saveIntoFile(DiagnosticsReport diagnosticsReport) {
    Optional<File> optionalTempDir = createTempDirIfNotExists();
    if (optionalTempDir.isPresent()) {
      File tempDir = optionalTempDir.get();
      File diagFile = new File(tempDir, "diagnostics.txt");
      write(diagnosticsReport, diagFile);
    }
  }

  private static void write(DiagnosticsReport diagnosticsReport, File diagFile) {
    byte[] diagReportAsBytes = diagnosticsReport.toString().getBytes(StandardCharsets.UTF_8);
    try {
      Files.write(diagFile.toPath(), diagReportAsBytes);
    } catch (IOException e) {
      startupLogger.error("Error occurred when writing diag report.", e);
    }
  }

  private static Optional<File> createTempDirIfNotExists() {
    String tempDirectory = System.getProperty("java.io.tmpdir");
    File folder = new File(tempDirectory, "applicationinsights");
    if (!folder.exists() && !folder.mkdirs()) {
      startupLogger.error("Failed to create directory: " + tempDirectory);
      return Optional.empty();
    }
    return Optional.of(folder);
  }

  @SuppressFBWarnings(
      value = "SECCI", // Command Injection
      justification = "No user data is used to construct the command below")
  private static String executeNativeMemoryDiag() {
    ProcessBuilder processBuilder =
        new ProcessBuilder("jcmd", pid(), "VM.native_memory", "summary");
    return CommandExecutor.executeWithoutException(processBuilder, startupLogger);
  }

  private static String pid() {
    return new PidFinder().getValue();
  }
}
