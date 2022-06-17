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

package io.opentelemetry.contrib.attach.override;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;

// Class to replace by an OTel class from the java contrib repo after next java contrib release
final class AgentFileProvider {

  static File getAgentFile() {

    verifyExistenceOfAgentJarFile();

    Path tempDirPath = createTempDir();

    Path tempAgentJarPath = createTempAgentJarFile(tempDirPath);

    deleteTempDirOnJvmExit(tempDirPath, tempAgentJarPath);

    return tempAgentJarPath.toFile();
  }

  private static void deleteTempDirOnJvmExit(Path tempDirPath, Path tempAgentJarPath) {
    tempAgentJarPath.toFile().deleteOnExit();
    tempDirPath.toFile().deleteOnExit();
  }

  private static void verifyExistenceOfAgentJarFile() {
    CodeSource codeSource = OpenTelemetryAgent.class.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      throw new IllegalStateException("could not get agent jar location");
    }
  }

  private static Path createTempDir() {
    Path tempDir;
    try {
      tempDir = Files.createTempDirectory("otel-agent");
    } catch (IOException e) {
      throw new IllegalStateException("Runtime attachment can't create temp directory", e);
    }
    return tempDir;
  }

  private static Path createTempAgentJarFile(Path tempDir) {
    URL url = OpenTelemetryAgent.class.getProtectionDomain().getCodeSource().getLocation();
    try {
      return copyTo(url, tempDir, "agent.jar");
    } catch (IOException e) {
      throw new IllegalStateException(
          "Runtime attachment can't create agent jar file in temp directory", e);
    }
  }

  private static Path copyTo(URL url, Path tempDir, String fileName) throws IOException {
    Path tempFile = tempDir.resolve(fileName);
    try (InputStream in = url.openStream()) {
      Files.copy(in, tempFile);
    }
    return tempFile;
  }

  private AgentFileProvider() {}
}
