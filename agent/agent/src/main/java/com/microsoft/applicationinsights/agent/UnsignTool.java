// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

class UnsignTool {

  private static final Pattern SIGNATURE_FILE = Pattern.compile("META-INF/.*\\.(RSA|SF)");

  static void run() throws IOException, URISyntaxException {
    Path signedPath = getAgentJar();
    Path unsignedPath = getUnsignedPath(signedPath);

    ScheduledExecutorService executor;
    try (JarInputStream in = new JarInputStream(Files.newInputStream(signedPath))) {

      Manifest signedManifest = in.getManifest();

      if (!isSigned(signedManifest)) {
        System.err.println();
        System.err.println(getRelativePath(signedPath) + " is already unsigned");
        in.close();
        System.exit(0);
      }

      executor = Executors.newSingleThreadScheduledExecutor();

      System.out.println();
      System.out.print("Unsigning...");
      System.out.flush();
      executor.scheduleWithFixedDelay(
          () -> {
            System.out.print(".");
            System.out.flush();
          },
          500,
          500,
          MILLISECONDS);

      unsign(in, signedManifest, unsignedPath);
    }

    shutdown(executor);

    System.err.println(); // for the "Unsigned......." line

    System.out.println();
    System.out.println("Unsigned agent jar: " + getRelativePath(unsignedPath));
  }

  private static void unsign(JarInputStream in, Manifest signedManifest, Path unsignedPath)
      throws IOException {

    Manifest unsignedManifest = new Manifest();
    unsignedManifest.getMainAttributes().putAll(signedManifest.getMainAttributes());

    try (JarOutputStream out =
        new JarOutputStream(Files.newOutputStream(unsignedPath), unsignedManifest)) {

      JarEntry entry;
      byte[] buffer = new byte[1024];
      while ((entry = in.getNextJarEntry()) != null) {
        if (SIGNATURE_FILE.matcher(entry.getName()).matches()) {
          continue;
        }
        out.putNextEntry(entry);
        int read;
        while ((read = in.read(buffer)) != -1) {
          out.write(buffer, 0, read);
        }
      }
    }
  }

  private static Path getUnsignedPath(Path agentJar) {
    String fileName = agentJar.getFileName().toString();
    if (!fileName.endsWith(".jar")) {
      throw new IllegalStateException("Unexpected agent jar file name: " + fileName);
    }
    String unsignedFileName = fileName.substring(0, fileName.length() - 4) + "-unsigned.jar";
    return agentJar.resolveSibling(unsignedFileName);
  }

  private static Path getRelativePath(Path unsignedAgentJar) {
    return Paths.get(".").toAbsolutePath().relativize(unsignedAgentJar.toAbsolutePath());
  }

  private static boolean isSigned(Manifest manifest) {
    for (Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
      for (Object key : entry.getValue().keySet()) {
        if (key instanceof Attributes.Name && key.toString().endsWith("-Digest")) {
          return true;
        }
      }
    }
    return false;
  }

  private static void shutdown(ScheduledExecutorService executor) {
    executor.shutdown();
    try {
      executor.awaitTermination(1, SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification = "The constructed file path will always point to the agent jar")
  private static Path getAgentJar() throws URISyntaxException {
    CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      throw new IllegalStateException("Could not get agent jar location");
    }
    return Paths.get(codeSource.getLocation().toURI());
  }

  private UnsignTool() {}
}
