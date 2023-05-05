// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
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

      jfrFile =
          Files.walk(new File("/tmp/root/applicationinsights").toPath())
              .filter(Files::isRegularFile)
              .filter(it -> it.toFile().getName().contains(".jfr"))
              .findFirst();

      if (!jfrFile.isPresent()) {
        Thread.sleep(1000, 0);
        continue;
      }

      File file = decompressFile(jfrFile.get().toFile());

      boolean hasTelemetry =
          com.microsoft.applicationinsights.jfrfile.JfrFileReader.hasEventOfType(
              file, "com.microsoft.applicationinsights.diagnostics.jfr.Telemetry");

      if (hasTelemetry) {
        return String.valueOf(true);
      }
    }

    return String.valueOf(false);
  }

  private File decompressFile(File jfrFile) throws Exception {
    File outFile;
    try {

      MessageDigest md = MessageDigest.getInstance("MD5");

      byte[] buffer = new byte[1024];
      try (GZIPInputStream stream = new GZIPInputStream(Files.newInputStream(jfrFile.toPath()))) {
        String name = jfrFile.getAbsolutePath();
        outFile = new File(name.substring(0, name.indexOf(".jfr") + 4));

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
          int len;
          while ((len = stream.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
            md.update(buffer);
          }
        }
      }

      return outFile;
    } catch (ZipException e) {
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
