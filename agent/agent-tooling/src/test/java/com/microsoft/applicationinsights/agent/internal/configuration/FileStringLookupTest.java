package com.microsoft.applicationinsights.agent.internal.configuration;

import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class FileStringLookupTest {

  private static final String CONNECTION_STRING = "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fake-ingestion-endpoint.example.com/";
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
