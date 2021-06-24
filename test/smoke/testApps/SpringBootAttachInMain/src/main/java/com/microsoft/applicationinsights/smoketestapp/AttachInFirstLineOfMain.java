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

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.agent.Agent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.springframework.lang.Nullable;

public class AttachInFirstLineOfMain {

  public static void attach() throws IOException {
    Path tempDir = Files.createTempDirectory("applicationinsights");
    Path agentJarFile = createTempAgentJarFile(tempDir);
    Path configFile = createTempConfigFile(tempDir);
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(tempDir, agentJarFile, configFile));
    ByteBuddyAgent.attach(agentJarFile.toFile(), getPid());
  }

  private static Path createTempAgentJarFile(Path tempDir) throws IOException {
    URL url = Agent.class.getProtectionDomain().getCodeSource().getLocation();
    return copyTo(url, tempDir, "applicationinsights-agent.jar");
  }

  private static Path createTempConfigFile(Path tempDir) throws IOException {
    URL url = AttachInFirstLineOfMain.class.getResource("/applicationinsights.json");
    if (url == null) {
      return null;
    }
    return copyTo(url, tempDir, "applicationinsights.json");
  }

  private static Path copyTo(URL url, Path tempDir, String fileName) throws IOException {
    Path tempFile = tempDir.resolveSibling(fileName);
    try (InputStream in = url.openStream()) {
      Files.copy(in, tempFile);
    }
    return tempFile;
  }

  private static String getPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private static class ShutdownHook extends Thread {

    private final Path directory;
    private final Path agentJarFile;
    @Nullable private final Path configFile;

    private ShutdownHook(Path directory, Path agentJarFile, Path configFile) {
      this.directory = directory;
      this.agentJarFile = agentJarFile;
      this.configFile = configFile;
    }

    public void run() {
      try {
        Files.delete(directory);
        Files.delete(agentJarFile);
        if (configFile != null) {
          Files.delete(configFile);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
