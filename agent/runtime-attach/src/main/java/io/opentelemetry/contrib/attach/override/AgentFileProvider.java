/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach.override;

import io.opentelemetry.contrib.attach.RuntimeAttach;
import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.logging.Level;
import java.util.logging.Logger;

// Class to replace by an OTel class from the java contrib repo after next java contrib release
final class AgentFileProvider {

  private static final Logger logger = Logger.getLogger(RuntimeAttach.class.getName());
  private Path tempDirPath;

  private Path agentJarPath;

  File getAgentFile() {

    verifyExistenceOfAgentJarFile();

    this.tempDirPath = createTempDir();

    return createTempAgentJarFile(tempDirPath);

  }

  void deleteTempDir() {
    try {
      Files.delete( this.tempDirPath);
      Files.delete(this.agentJarPath);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Error during deletion of the temp dir used by the runtime attachement", e);
    }
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

  private File createTempAgentJarFile(Path tempDir) {
    URL url = OpenTelemetryAgent.class.getProtectionDomain().getCodeSource().getLocation();
    try {
      this.agentJarPath = copyTo(url, tempDir, "agent.jar");
    } catch (IOException e) {
      throw new IllegalStateException("Runtime attachment can't create agent jar file in temp directory", e);
    }
    return agentJarPath.toFile();
  }

  private static Path copyTo(URL url, Path tempDir, String fileName) throws IOException {
    Path tempFile = tempDir.resolveSibling(fileName);
    try (InputStream in = url.openStream()) {
      Files.copy(in, tempFile);
    }
    return tempFile;
  }

  AgentFileProvider() {}
}
