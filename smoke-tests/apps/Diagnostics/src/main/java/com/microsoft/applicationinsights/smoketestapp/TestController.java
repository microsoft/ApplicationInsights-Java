// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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
    Optional<Path> jfrFile;
    for (int i = 0; i < 60; i++) {
      try {
        jfrFile =
            Files.walk(new File("/tmp/root/applicationinsights").toPath())
                .filter(Files::isRegularFile)
                .filter(it -> it.toFile().getName().contains(".jfr"))
                .findFirst();

        if (!jfrFile.isPresent()) {
          Thread.sleep(1000, 0);
          continue;
        }

        Path decompressedFile = decompressFile(jfrFile.get());

        boolean hasTelemetry =
            com.microsoft.applicationinsights.jfrfile.JfrFileReader.hasEventOfType(
                decompressedFile, "com.microsoft.applicationinsights.diagnostics.jfr.Telemetry");

        if (hasTelemetry) {
          return String.valueOf(true);
        }
      } catch (Exception e) {
        // Ignore early exceptions, as to be expected, throw them if they are still happening
        // towards the end
        if (i > 55) {
          throw e;
        } else {
          Thread.sleep(1000, 0);
        }
      }
    }

    return String.valueOf(false);
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
