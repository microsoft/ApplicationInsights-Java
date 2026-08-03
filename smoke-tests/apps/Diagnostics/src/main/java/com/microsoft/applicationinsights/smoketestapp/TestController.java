// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.File;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/jfrFileHasDiagnostics")
  public String jfrFileHasDiagnostics() throws Exception {
    return String.valueOf(
        pollForJfrFileMatching(
            decompressedFile ->
                com.microsoft.applicationinsights.jfrfile.JfrFileReader.hasEventOfType(
                    decompressedFile,
                    "com.microsoft.applicationinsights.diagnostics.jfr.Telemetry")));
  }

  /**
   * Verifies that a continuous-profiling recording captured the breach diagnostic events. When a
   * profile is requested the profiler immediately dumps the backward-looking circular buffer, so
   * {@code AlertBreach}, {@code MachineInfo} and {@code CGroupData} must be emitted before the dump
   * in order to be captured in the recording.
   */
  @GetMapping("/continuousJfrFileHasDiagnostics")
  public String continuousJfrFileHasDiagnostics() throws Exception {
    String[] requiredEvents = {
      "com.microsoft.applicationinsights.diagnostics.jfr.AlertBreach",
      "com.microsoft.applicationinsights.diagnostics.jfr.MachineInfo",
      "com.microsoft.applicationinsights.diagnostics.jfr.CGroupData",
    };

    return String.valueOf(
        pollForJfrFileMatching(
            decompressedFile -> {
              for (String event : requiredEvents) {
                if (!com.microsoft.applicationinsights.jfrfile.JfrFileReader.hasEventInstanceOfType(
                    decompressedFile, event)) {
                  return false;
                }
              }
              return true;
            }));
  }

  /**
   * Polls for up to 60 seconds for a {@code .jfr} file produced by the agent that satisfies {@code
   * predicate}. Continuous profiling can produce several dumps and a dump may be observed
   * mid-write, so every candidate file is examined (newest first) and any file that cannot be read
   * yet is skipped rather than failing the whole request.
   */
  private boolean pollForJfrFileMatching(JfrFilePredicate predicate) throws Exception {
    for (int i = 0; i < 60; i++) {
      for (Path jfrFile : findJfrFilesNewestFirst()) {
        try {
          Path decompressedFile = decompressFile(jfrFile);
          if (predicate.test(decompressedFile)) {
            return true;
          }
        } catch (Exception e) {
          // A dump may still be being written, or otherwise unreadable; skip it and try the next
          // candidate / next poll iteration.
          if (i > 55) {
            e.printStackTrace();
          }
        }
      }
      Thread.sleep(1000, 0);
    }

    return false;
  }

  @FunctionalInterface
  private interface JfrFilePredicate {
    boolean test(Path decompressedFile) throws Exception;
  }

  private static List<Path> findJfrFilesNewestFirst() {
    Path root = new File("/tmp/root/applicationinsights").toPath();
    if (!Files.isDirectory(root)) {
      return Collections.emptyList();
    }
    try (Stream<Path> files = Files.walk(root)) {
      return files
          .filter(Files::isRegularFile)
          .filter(it -> it.toFile().getName().contains(".jfr"))
          .sorted(Comparator.comparingLong((Path it) -> it.toFile().lastModified()).reversed())
          .collect(Collectors.toList());
    } catch (java.io.IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Path decompressFile(Path jfrFile) {
    try {
      byte[] buffer = new byte[1024];
      try (GZIPInputStream stream = new GZIPInputStream(Files.newInputStream(jfrFile))) {
        Path outFile = Files.createTempFile("", ".jfr");
        try (OutputStream fos = Files.newOutputStream(outFile)) {
          int len;
          while ((len = stream.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
        }
        return outFile;
      }
    } catch (ZipException e) {
      e.printStackTrace();
      return jfrFile;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @GetMapping("/api/profileragent/v4/settings")
  public String profilerConfig() {
    return "OK";
  }
}
