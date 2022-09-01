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
    if (args.length == 1 && args[0].equals("unsign")) {
      unsign();
    } else {
      System.err.println("Incorrect usage");
    }
  }

  private static void unsign() throws IOException, URISyntaxException {
    Path agentJar = getAgentJar();

    String fileName = agentJar.getFileName().toString();
    if (!fileName.endsWith(".jar")) {
      System.err.println("Unexpected agent jar file name: " + fileName);
      return;
    }
    String unsignedFileName = fileName.substring(0, fileName.length() - 4) + "-unsigned.jar";
    Path unsignedAgentJar = agentJar.resolveSibling(unsignedFileName);

    ScheduledExecutorService executor;
    byte[] buffer = new byte[1024];
    try (JarInputStream in = new JarInputStream(Files.newInputStream(agentJar))) {

      Manifest inManifest = in.getManifest();

      if (!isSigned(inManifest)) {
        System.err.println();
        System.err.println(getRelativePath(agentJar) + " is already unsigned");
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

      Manifest outManifest = new Manifest();
      outManifest.getMainAttributes().putAll(inManifest.getMainAttributes());

      JarOutputStream out =
          new JarOutputStream(Files.newOutputStream(unsignedAgentJar), outManifest);

      JarEntry entry;
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

      out.close();
    }

    shutdown(executor);

    System.err.println(); // for the "Unsigned......." line

    System.out.println();
    System.out.println("Unsigned agent jar: " + getRelativePath(unsignedAgentJar));
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
      throw new IllegalStateException("could not get agent jar location");
    }

    return Paths.get(codeSource.getLocation().toURI());
  }

  private Main() {}
}
