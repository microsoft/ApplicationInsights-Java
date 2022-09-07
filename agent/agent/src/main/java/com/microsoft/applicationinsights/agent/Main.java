/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

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

@SuppressWarnings("SystemOut")
public class Main {

  private static final Pattern SIGNATURE_FILE = Pattern.compile("META-INF/.*\\.(RSA|SF)");

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("No command specified");
      return;
    }
    String command = args[0];
    if (command.equals("unsign")) {
      if (args.length > 1) {
        System.err.println("The unsign command does not expect any arguments");
        return;
      }
      unsign();
      return;
    }
    System.err.println("Unknown command: " + command);
  }

  private static void unsign() throws IOException, URISyntaxException {
    Path signedPath = getAgentJar();
    Path unsignedPath = getUnsignedPath(signedPath);

    ScheduledExecutorService executor;
    try (JarInputStream in = new JarInputStream(Files.newInputStream(signedPath))) {

      Manifest signedManifest = in.getManifest();

      if (!isSigned(signedManifest)) {
        System.err.println();
        System.err.println(getRelativePath(signedPath) + " is already unsigned");
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

  private static Path getAgentJar() throws URISyntaxException {
    CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      throw new IllegalStateException("Could not get agent jar location");
    }
    return Paths.get(codeSource.getLocation().toURI());
  }

  private Main() {}
}
