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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import org.apache.commons.text.StringSubstitutor;
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
    file = File.createTempFile("test", "", tempFolder);
    Writer writer = Files.newBufferedWriter(file.toPath(), UTF_8);
    writer.write(CONNECTION_STRING);
    writer.close();

    stringSubstitutor = new StringSubstitutor(FileStringLookup.INSTANCE);
    stringSubstitutor.setEnableSubstitutionInVariables(true);
  }

  @Test
  public void testGoodFileLookupFormat() {
    String connectionString = "${file:" + file.getPath() + "}";
    String value = stringSubstitutor.replace(connectionString);
    assertThat(value).isEqualTo(CONNECTION_STRING);
  }

  @Test
  public void testBadFileLookupFormat() {
    String connectionString = "file:" + file.getPath();
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
}
