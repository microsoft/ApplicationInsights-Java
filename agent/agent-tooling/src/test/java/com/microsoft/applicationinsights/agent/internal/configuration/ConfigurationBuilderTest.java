// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationBuilderTest {

  private static final String CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fake-ingestion-endpoint";
  private File connectionStringFile;
  @TempDir File temp;

  // TODO (heya) clean up the rest of resource files. We can create them at test runtime. Be
  // consistent with connectionStringFile.
  @BeforeEach
  public void setup() throws IOException {
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

    ConfigurationBuilder.overlayFromEnv(config);

    assertThat(config.connectionString).isEqualTo(testConnectionString);
    assertThat(config.sampling.percentage).isEqualTo(testSamplingPercentage);
  }

  @Test
  void testRpConfigurationOverlayWithEnvVarAndSysPropPopulated() throws Exception {
    String testConnectionString = "test-connection-string";
    double testSamplingPercentage = 10.0;

    withEnvironmentVariable("APPLICATIONINSIGHTS_CONNECTION_STRING", testConnectionString)
        .and("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", String.valueOf(testSamplingPercentage))
        .execute(
            () -> {
              RpConfiguration config = new RpConfiguration();

              config.connectionString = String.format("original-%s", testConnectionString);
              config.sampling.percentage = testSamplingPercentage + 1.0;

              ConfigurationBuilder.overlayFromEnv(config);

              assertThat(config.connectionString).isEqualTo(testConnectionString);
              assertThat(config.sampling.percentage).isEqualTo(testSamplingPercentage);
            });
  }

  @Test
  void testOverlayWithEnvVarWithGoodFileStringLookupFormat() throws Exception {
    Configuration configuration = new Configuration();
    configuration.connectionString = "${file:" + connectionStringFile.getAbsolutePath() + "}";
    ConfigurationBuilder.overlayFromEnv(configuration, Paths.get("."));
    assertThat(configuration.connectionString).isEqualTo(CONNECTION_STRING);
  }

  @Test
  void testOverlayWithEnvVarWithBadFileStringLookupFormat() throws Exception {
    Configuration configuration = new Configuration();
    configuration.connectionString = "${file:" + connectionStringFile.getAbsolutePath();
    ConfigurationBuilder.overlayFromEnv(configuration, Paths.get("."));
    assertThat(configuration.connectionString).isEqualTo(configuration.connectionString);

    configuration.connectionString = "${xyz:" + connectionStringFile.getAbsolutePath() + "}";
    ConfigurationBuilder.overlayFromEnv(configuration, Paths.get("."));
    assertThat(configuration.connectionString).isEqualTo(configuration.connectionString);

    configuration.connectionString = "file:" + connectionStringFile.getAbsolutePath() + "}";
    ConfigurationBuilder.overlayFromEnv(configuration, Paths.get("."));
    assertThat(configuration.connectionString).isEqualTo(configuration.connectionString);

    configuration.connectionString = "file:" + connectionStringFile.getAbsolutePath();
    ConfigurationBuilder.overlayFromEnv(configuration, Paths.get("."));
    assertThat(configuration.connectionString).isEqualTo(configuration.connectionString);

    configuration.connectionString = CONNECTION_STRING;
    ConfigurationBuilder.overlayFromEnv(configuration, Paths.get("."));
    assertThat(configuration.connectionString).isEqualTo(configuration.connectionString);
  }

  @Test
  void testConnectionStringEnvVarHasHigherPrecedenceOverFileLookup() throws Exception {
    String testConnectionString = "test-connection-string";
    withEnvironmentVariable("APPLICATIONINSIGHTS_CONNECTION_STRING", testConnectionString)
        .execute(
            () -> {
              Configuration configuration = new Configuration();

              configuration.connectionString =
                  "${file:" + connectionStringFile.getAbsolutePath() + "}";
              ConfigurationBuilder.overlayFromEnv(configuration, Paths.get("."));

              assertThat(configuration.connectionString).isEqualTo(testConnectionString);
            });
  }

  @Test
  void testProxyEnvOverlay() throws Exception {
    withEnvironmentVariable("APPLICATIONINSIGHTS_PROXY", "https://me:passw@host:1234")
        .execute(
            () -> {
              Configuration configuration = new Configuration();

              configuration.proxy.host = "old";
              configuration.proxy.port = 555;
              ConfigurationBuilder.overlayFromEnv(configuration, Paths.get("."));

              assertThat(configuration.proxy.host).isEqualTo("host");
              assertThat(configuration.proxy.port).isEqualTo(1234);
              assertThat(configuration.proxy.username).isEqualTo("me");
              assertThat(configuration.proxy.password).isEqualTo("passw");
            });
  }

  private static void runProfilerEnvOverlay(boolean fileValue, boolean expected) {
    Configuration configuration = new Configuration();
    configuration.preview.profiler.enabled = fileValue;
    ConfigurationBuilder.overlayProfilerEnvVars(configuration);
    assertThat(configuration.preview.profiler.enabled).isEqualTo(expected);
  }

  @Test
  void testProfilerEnvOverlay() throws Exception {
    // Enabled in file overlayed false is disabled
    withEnvironmentVariable("APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED", "false")
        .execute(() -> runProfilerEnvOverlay(true, false));

    // Disabled in file overlayed true is enabled
    withEnvironmentVariable("APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED", "true")
        .execute(() -> runProfilerEnvOverlay(false, true));
  }
}
