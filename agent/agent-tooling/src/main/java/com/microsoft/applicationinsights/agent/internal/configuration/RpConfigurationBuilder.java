// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import javax.annotation.Nullable;

public class RpConfigurationBuilder {

  private static final String APPLICATIONINSIGHTS_RP_CONFIGURATION_FILE =
      "APPLICATIONINSIGHTS_RP_CONFIGURATION_FILE";

  @Nullable
  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification =
          "The constructed file path cannot be controlled by an end user of the instrumented application")
  public static RpConfiguration create(Path agentJarPath) throws IOException {
    Path configPath;
    String configPathString =
        ConfigurationBuilder.getEnvVar(APPLICATIONINSIGHTS_RP_CONFIGURATION_FILE);

    if (configPathString != null) {
      configPath = new File(configPathString).toPath();
    } else {
      configPath = agentJarPath.resolveSibling("applicationinsights-rp.json");
    }

    if (Files.exists(configPath)) {
      return loadJsonConfigFile(configPath);
    }
    return null;
  }

  public static RpConfiguration loadJsonConfigFile(Path configPath) throws IOException {
    if (!Files.exists(configPath)) {
      throw new IllegalStateException("rp config file does not exist: " + configPath);
    }

    BasicFileAttributes attributes = Files.readAttributes(configPath, BasicFileAttributes.class);
    // important to read last modified before reading the file, to prevent possible race condition
    // where file is updated after reading it but before reading last modified, and then since
    // last modified doesn't change after that, the new updated file will not be read afterwards
    long lastModifiedTime = attributes.lastModifiedTime().toMillis();
    RpConfiguration configuration = getConfigurationFromConfigFile(configPath);
    configuration.configPath = configPath;
    configuration.lastModifiedTime = lastModifiedTime;
    return configuration;
  }

  private static RpConfiguration getConfigurationFromConfigFile(Path configPath)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try (InputStream in = Files.newInputStream(configPath)) {
      return mapper.readValue(in, RpConfiguration.class);
    }
  }

  private RpConfigurationBuilder() {}
}
