// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileStringLookupTest {

  private static final String CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fake-ingestion-endpoint.example.com/";
  private File file;
  private StringSubstitutor stringSubstitutor;

  @TempDir File tempFolder;

  @BeforeEach
  public void setup() throws IOException {
    file = File.createTempFile("test-", ".txt", tempFolder);
    Writer writer = Files.newBufferedWriter(file.toPath(), UTF_8);
    writer.write(CONNECTION_STRING);
    writer.close();

    assertThat(file.exists()).isTrue();
    Map<String, StringLookup> stringLookupMap =
        Collections.singletonMap(
            StringLookupFactory.KEY_FILE, new FileStringLookup(tempFolder.toPath()));
    StringLookup stringLookup =
        StringLookupFactory.INSTANCE.interpolatorStringLookup(stringLookupMap, null, false);
    stringSubstitutor = new StringSubstitutor(stringLookup);
  }

  @AfterEach
  public void cleanup() throws IOException {
    Files.delete(file.toPath());
  }

  @Test
  public void testGoodAbsoluteFileLookupFormat() {
    String connectionString = "${file:" + file.getAbsolutePath() + "}";
    String value = stringSubstitutor.replace(connectionString);
    assertThat(value).isEqualTo(CONNECTION_STRING);
  }

  @Test
  public void testGoodRelativeFileLookupFormat() {
    String connectionString = "${file:" + file.getName() + "}";
    String value = stringSubstitutor.replace(connectionString);
    assertThat(value).isEqualTo(CONNECTION_STRING);
  }

  @Test
  public void testOtherKeyFileLookupWillFail() {
    String connectionString = "${xyz:" + file.getAbsolutePath() + "}";
    String value = stringSubstitutor.replace(connectionString);
    assertThat(value).isNotEqualTo(CONNECTION_STRING);
    assertThat(value).isEqualTo(connectionString);
  }

  @Test
  public void testBadFileLookupFormat() {
    String connectionString = "file:" + file.getAbsolutePath();
    String value = stringSubstitutor.replace(connectionString);
    assertThat(value).isNotEqualTo(CONNECTION_STRING);
    assertThat(value).isEqualTo(connectionString);
  }

  @Test
  public void testEmptyFileLookup() {
    String connectionString = "";
    String value = stringSubstitutor.replace(connectionString);
    assertThat(value).isEmpty();
  }

  @Test
  public void testNullFileLookup() {
    String connectionString = null;
    String value = stringSubstitutor.replace(connectionString);
    assertThat(value).isNull();
  }

  @Test
  public void testValidConnectionString() {
    String connectionString = "InstrumentationKey=00000-000000-000000-0000;";
    String value = stringSubstitutor.replace(connectionString);
    assertThat(value).isEqualTo(connectionString);
  }

  @Test
  public void testThrowIllegalArgumentException() {
    String connectionString = "${file:file.txt}";
    assertThatThrownBy(() -> stringSubstitutor.replace(connectionString))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testRelativePath() {
    String connectionString = "${file:./" + file.getName() + "}";
    String value = stringSubstitutor.replace(connectionString);
    assertThat(value).isEqualTo(CONNECTION_STRING);
  }
}
