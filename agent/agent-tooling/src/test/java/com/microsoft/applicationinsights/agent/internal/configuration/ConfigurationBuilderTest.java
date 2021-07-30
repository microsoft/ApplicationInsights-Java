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

package com.microsoft.applicationinsights.agent.internal.configuration;

import static com.microsoft.applicationinsights.agent.internal.configuration.ConfigurationBuilder.cleanJsonContent;
import static com.microsoft.applicationinsights.agent.internal.configuration.ConfigurationBuilder.trimAndEmptyToNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ConfigurationBuilderTest {

  Path getConfigFilePath(String resourceName) {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource(resourceName).getFile());
    return file.toPath();
  }

  @Test
  void testEmptyToNull() {
    assertThat(trimAndEmptyToNull("   ")).isNull();
    assertThat(trimAndEmptyToNull("")).isNull();
    assertThat(trimAndEmptyToNull(null)).isNull();
    assertThat(trimAndEmptyToNull("a")).isEqualTo("a");
    assertThat(trimAndEmptyToNull("  a  ")).isEqualTo("a");
    assertThat(trimAndEmptyToNull("\t")).isNull();
  }

  @Test
  void testGetConfigFilePath() {
    Path path = getConfigFilePath("applicationinsights.json");
    assertThat(path.toString().endsWith("applicationinsights.json")).isTrue();
  }

  @Test
  void testValidJson() throws IOException {
    Path path = getConfigFilePath("applicationinsights.json");
    Configuration configuration = ConfigurationBuilder.getConfigurationFromConfigFile(path, true);
    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
    assertThat(configuration.role.name).isEqualTo("Something Good");
    assertThat(configuration.role.instance).isEqualTo("xyz123");
  }

  @Test
  void testFaultyJson() throws IOException {
    Path path = getConfigFilePath("applicationinsights_faulty.json");
    Configuration configuration = ConfigurationBuilder.getConfigurationFromConfigFile(path, true);
    // Configuration object should still be created.
    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
    assertThat(configuration.role.name).isEqualTo("Something Good");
  }

  @Test
  void testMalformedJson() {
    Path path = getConfigFilePath("applicationinsights_malformed.json");

    assertThatThrownBy(() -> ConfigurationBuilder.getConfigurationFromConfigFile(path, true))
        .isInstanceOf(FriendlyException.class)
        .hasMessageContaining("has a malformed JSON at path $.role.");
  }

  @Test
  void testMalformedFaultyJson() {
    Path path = getConfigFilePath("applicationinsights_malformed_faulty.json");

    assertThatThrownBy(() -> ConfigurationBuilder.getConfigurationFromConfigFile(path, true))
        .isInstanceOf(FriendlyException.class)
        .hasMessageContaining("has a malformed JSON")
        .hasMessageNotContaining("has a malformed JSON at path $.null.");
  }

  @Test
  void testLenientMalformedJson() throws IOException {
    Path path = getConfigFilePath("applicationinsights_lenient_malformed.json");
    Configuration configuration = ConfigurationBuilder.getConfigurationFromConfigFile(path, true);
    // Configuration object should still be created.
    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
    assertThat(configuration.role.name).isEqualTo("Something Good");
  }

  @Test
  void testGetJsonEncodingExceptionMessage() {
    String pathNull =
        ConfigurationBuilder.getJsonEncodingExceptionMessage("file path/to/file", null);
    String pathEmpty =
        ConfigurationBuilder.getJsonEncodingExceptionMessage("file path/to/file", "");
    String pathValid =
        ConfigurationBuilder.getJsonEncodingExceptionMessage(
            "file path/to/file", "has a malformed JSON at path $.role.");
    String pathInvalidAndNull =
        ConfigurationBuilder.getJsonEncodingExceptionMessage(
            "file path/to/file", "has a malformed JSON at path $.null.[0]");
    String pathInvalid =
        ConfigurationBuilder.getJsonEncodingExceptionMessage(
            "file path/to/file", "has a malformed JSON at path $.");
    assertThat(pathNull)
        .isEqualTo(
            "Application Insights Java agent's configuration file path/to/file has a malformed JSON\n");
    assertThat(pathEmpty)
        .isEqualTo(
            "Application Insights Java agent's configuration file path/to/file has a malformed JSON\n");
    assertThat(pathValid)
        .isEqualTo(
            "Application Insights Java agent's configuration file path/to/file has a malformed JSON at path $.role.\n");
    assertThat(pathInvalid)
        .isEqualTo(
            "Application Insights Java agent's configuration file path/to/file has a malformed JSON\n");
    assertThat(pathInvalidAndNull)
        .isEqualTo(
            "Application Insights Java agent's configuration file path/to/file has a malformed JSON\n");
  }

  @Test
  void testCleanJsonConetent() throws IOException {
    Path path = getConfigFilePath("applicationinsights.json");
    Path malformedPath = getConfigFilePath("applicationinsights_lenient_malformed.json");
    String jsonContent =
        cleanJsonContent(new String(Files.readAllBytes(path), Charset.defaultCharset()));
    String cleanJsonContent =
        cleanJsonContent(new String(Files.readAllBytes(malformedPath), Charset.defaultCharset()));
    assertThat(cleanJsonContent).isEqualTo(jsonContent);
  }
}
