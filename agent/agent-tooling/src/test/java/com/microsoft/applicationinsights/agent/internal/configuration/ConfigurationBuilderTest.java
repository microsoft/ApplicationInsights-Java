// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationBuilderTest {

  private static final String CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fake-ingestion-endpoint";
  private File connectionStringFile;
  @TempDir File temp;

  private static final Map<String, String> envVars = new HashMap<>();
  private static final Map<String, String> systemProperties = new HashMap<>();

  // TODO (heya) clean up the rest of resource files. We can create them at test runtime. Be
  // consistent with connectionStringFile.
  @BeforeEach
  public void setup() throws IOException {
    envVars.clear();
    systemProperties.clear();
    connectionStringFile = File.createTempFile("test", ".txt", temp);
    Writer writer = Files.newBufferedWriter(connectionStringFile.toPath(), UTF_8);
    writer.write(CONNECTION_STRING);
    writer.close();

    assertThat(connectionStringFile.exists()).isTrue();
  }

  @AfterEach
  public void cleanup() throws IOException {
    Files.delete(connectionStringFile.toPath());
  }

  Path getConfigFilePath(String resourceName) {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource(resourceName).getFile());
    return file.toPath();
  }

  @Test
  void testGetConfigFilePath() {
    Path path = getConfigFilePath("applicationinsights.json");
    assertThat(path.toString().endsWith("applicationinsights.json")).isTrue();
  }

  @Test
  void testValidJson() {
    Path path = getConfigFilePath("applicationinsights.json");
    Configuration configuration = ConfigurationBuilder.getConfigurationFromConfigFile(path);
    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
    assertThat(configuration.role.name).isEqualTo("Something Good");
    assertThat(configuration.role.instance).isEqualTo("xyz123");
  }

  @Test
  void testJsonWithUtf8ByteOrderMarker() {
    Path path = getConfigFilePath("applicationinsights_with_utf8_bom.json");
    Configuration configuration = ConfigurationBuilder.getConfigurationFromConfigFile(path);
    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
  }

  @Test
  void testFaultyJson() {
    Path path = getConfigFilePath("applicationinsights_faulty.json");
    Configuration configuration = ConfigurationBuilder.getConfigurationFromConfigFile(path);
    // Configuration object should still be created.
    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
    assertThat(configuration.role.name).isEqualTo("Something Good");
  }

  @Test
  void testMalformedJson() {
    Path path = getConfigFilePath("applicationinsights_malformed.json");
    assertThatThrownBy(() -> ConfigurationBuilder.getConfigurationFromConfigFile(path))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void testMalformedFaultyJson() {
    Path path = getConfigFilePath("applicationinsights_malformed_faulty.json");
    assertThatThrownBy(() -> ConfigurationBuilder.getConfigurationFromConfigFile(path))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void testMalformedJsonWithUnicode() {
    Path path = getConfigFilePath("applicationinsights_malformed_unicode.json");
    assertThatThrownBy(() -> ConfigurationBuilder.getConfigurationFromConfigFile(path))
        .isInstanceOf(FriendlyException.class);
  }

  @Test
  void testGetJsonEncodingExceptionMessage() {
    String pathNull =
        ConfigurationBuilder.getJsonEncodingExceptionMessage(null, "file path/to/file");
    String pathEmpty =
        ConfigurationBuilder.getJsonEncodingExceptionMessage("", "file path/to/file");
    assertThat(pathNull).isEqualTo("The configuration file path/to/file contains malformed JSON");
    assertThat(pathEmpty).isEqualTo("The configuration file path/to/file contains malformed JSON");
  }

  @Test
  void testRpConfigurationOverlayWithEnvVarAndSysPropUnchanged() {
    String testConnectionString = "test-connection-string";
    double testSamplingPercentage = 10.0;
    RpConfiguration config = new RpConfiguration();

    config.connectionString = testConnectionString;
    config.sampling.percentage = testSamplingPercentage;

    ConfigurationBuilder.overlayFromEnv(config, this::envVars, this::systemProperties);

    assertThat(config.connectionString).isEqualTo(testConnectionString);
    assertThat(config.sampling.percentage).isEqualTo(testSamplingPercentage);
  }

  @Test
  void testRpConfigurationOverlayWithEnvVarAndSysPropPopulated() throws Exception {
    String testConnectionString = "test-connection-string";
    double testSamplingPercentage = 10.0;
    envVars.put("APPLICATIONINSIGHTS_CONNECTION_STRING", testConnectionString);
    envVars.put("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", String.valueOf(testSamplingPercentage));
    RpConfiguration config = new RpConfiguration();

    config.connectionString = String.format("original-%s", testConnectionString);
    config.sampling.percentage = testSamplingPercentage + 1.0;

    ConfigurationBuilder.overlayFromEnv(config, this::envVars, this::systemProperties);

    assertThat(config.connectionString).isEqualTo(testConnectionString);
    assertThat(config.sampling.percentage).isEqualTo(testSamplingPercentage);
  }

  @SuppressWarnings("MethodCanBeStatic")
  private String envVars(String key) {
    return envVars.get(key);
  }

  @SuppressWarnings("MethodCanBeStatic")
  private String systemProperties(String key) {
    return systemProperties.get(key);
  }

  @Test
  void testOverlayWithEnvVarWithGoodFileStringLookupFormat() throws Exception {
    Configuration configuration = new Configuration();
    configuration.connectionString = "${file:" + connectionStringFile.getAbsolutePath() + "}";
    ConfigurationBuilder.overlayFromEnv(
        configuration, Paths.get("."), this::envVars, this::systemProperties);
    assertThat(configuration.connectionString).isEqualTo(CONNECTION_STRING);
  }

  @Test
  void testConnectionStringEnvVarHasHigherPrecedenceOverFileLookup() throws Exception {
    String testConnectionString = "InstrumentationKey=00000000-0000-0000-0000-000000000000";
    envVars.put("APPLICATIONINSIGHTS_CONNECTION_STRING", testConnectionString);

    Configuration configuration = new Configuration();

    configuration.connectionString = "${file:" + connectionStringFile.getAbsolutePath() + "}";
    ConfigurationBuilder.overlayFromEnv(
        configuration, Paths.get("."), this::envVars, this::systemProperties);

    assertThat(configuration.connectionString).isEqualTo(testConnectionString);
  }

  @Test
  void testProxyEnvOverlay() throws Exception {
    envVars.put("APPLICATIONINSIGHTS_PROXY", "https://me:passw@host:1234");
    Configuration configuration = new Configuration();

    configuration.proxy.host = "old";
    configuration.proxy.port = 555;
    ConfigurationBuilder.overlayFromEnv(
        configuration, Paths.get("."), this::envVars, this::systemProperties);

    assertThat(configuration.proxy.host).isEqualTo("host");
    assertThat(configuration.proxy.port).isEqualTo(1234);
    assertThat(configuration.proxy.username).isEqualTo("me");
    assertThat(configuration.proxy.password).isEqualTo("passw");
  }

  private void runProfilerEnvOverlay(boolean fileValue, boolean expected) {
    Configuration configuration = new Configuration();
    configuration.preview.profiler.enabled = fileValue;
    ConfigurationBuilder.overlayProfilerEnvVars(configuration, this::envVars);
    assertThat(configuration.preview.profiler.enabled).isEqualTo(expected);
  }

  @Test
  void testProfilerEnvOverlay() throws Exception {
    // Enabled in file overlayed false is disabled
    envVars.put("APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED", "false");
    runProfilerEnvOverlay(true, false);

    // Disabled in file overlayed true is enabled
    envVars.put("APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED", "true");
    runProfilerEnvOverlay(false, true);
  }
}
