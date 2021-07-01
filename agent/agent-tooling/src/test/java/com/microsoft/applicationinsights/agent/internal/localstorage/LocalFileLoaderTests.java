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

package com.microsoft.applicationinsights.agent.internal.localstorage;

import static com.microsoft.applicationinsights.agent.internal.localstorage.PersistenceHelper.DEFAULT_FOLDER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class LocalFileLoaderTests {

  private static final String BYTE_BUFFERS_TEST_FILE = "read-transmission.txt";
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final File PERSISTED_FILE = new File(DEFAULT_FOLDER, BYTE_BUFFERS_TEST_FILE);

  @AfterEach
  public void cleanup() {
    if (PERSISTED_FILE.exists()) {
      assertThat(PERSISTED_FILE.delete()).isTrue();
    }
  }

  @Test
  public void testLoadFile() throws IOException {
    File sourceFile =
        new File(getClass().getClassLoader().getResource(BYTE_BUFFERS_TEST_FILE).getPath());

    /*
     * move this file to {@link DEFAULT_FOlDER} if it doesn't exist yet.
     */
    if (!PERSISTED_FILE.exists()) {
      FileUtils.moveFile(sourceFile, PERSISTED_FILE);
    }
    assertThat(PERSISTED_FILE.exists()).isTrue();

    LocalFileCache localFileCache = new LocalFileCache();
    localFileCache.addPersistedFilenameToMap(BYTE_BUFFERS_TEST_FILE);

    LocalFileLoader localFileLoader = new LocalFileLoader(localFileCache);
    String bytesString = readTelemetriesFromDiskToString(localFileLoader);

    String[] stringArray = bytesString.split("\n");
    assertThat(stringArray.length).isEqualTo(10);

    for (int i = 0; i < stringArray.length; i++) {
      JsonNode jsonNode = MAPPER.readTree(stringArray[i]);

      // verify common properties
      assertThat(jsonNode).hasSize(7);
      assertThat(jsonNode.get("ver").asInt()).isEqualTo(1);
      verifyTelemetryName(i, jsonNode.get("name").asText());
      verifyTelemetryTime(i, jsonNode.get("time").asText());
      assertThat(jsonNode.get("sampleRate").asInt()).isEqualTo(100);
      assertThat(jsonNode.get("iKey").asText()).isEqualTo("00000000-0000-0000-0000-0FEEDDADBEEF");

      // verify tags
      JsonNode tagsNode = jsonNode.get("tags");
      verifyTagsNodeSize(i, tagsNode.size());

      assertThat(tagsNode.get("ai.internal.sdkVersion").asText()).isEqualTo("java:3.1.1");
      assertThat(tagsNode.get("ai.internal.nodeName").asText()).isEqualTo("test-role-name");
      assertThat(tagsNode.get("ai.cloud.roleInstance").asText()).isEqualTo("test-role-instance");
      if (i == 8) { // RemoteDependency
        assertThat(tagsNode.get("ai.operation.id").asText())
            .isEqualTo("891b332db33c65cc6497c014f02db26d");
      } else if (i == 9) {
        assertThat(tagsNode.get("ai.operation.id").asText())
            .isEqualTo("0cb22c0f071802f7f314569b007c9a1e");
        assertThat(tagsNode.get("ai.operation.name").asText()).isEqualTo("GET /webjars/**");
        assertThat(tagsNode.get("ai.user.userAgent").asText())
            .isEqualTo(
                "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Safari/537.36");
      }

      // verify data
      JsonNode data = jsonNode.get("data");
      verifyDataBaseType(i, data.get("baseType").asText());

      JsonNode baseData = data.get("baseData");
      assertThat(baseData.get("ver").asInt()).isEqualTo(2);
      JsonNode metrics = baseData.get("metrics");

      if (i < 7) { // metrics is only applicable to Metric Telemetry type
        verifyMetricsName(i, metrics.get(0).get("name").asText());
        assertThat(metrics.get(0).get("kind").asInt()).isEqualTo(0);
        verifyMetricsValue(i, metrics.get(0).get("value").asInt());
      }

      if (i == 7) { // Message
        assertThat(baseData.get("message").asText())
            .isEqualTo("Tomcat initialized with port(s): 8080 (http)");
        assertThat(baseData.get("severityLevel").asText()).isEqualTo("Information");
      }

      if (i == 8) { // RemoteDependency's baseData
        verifyRemoteDependencyBaseData(baseData);
      }

      if (i == 9) { // Request's baseData
        verifyRequestBaseData(baseData);
      }

      // verify properties
      verifyProperties(i, baseData.get("properties"));
    }
  }

  @Test
  public void testWriteAndReadRandomText() {
    String text = "hello world";
    LocalFileCache cache = new LocalFileCache();
    LocalFileWriter writer = new LocalFileWriter(cache);
    writer.writeToDisk(singletonList(ByteBuffer.wrap(text.getBytes(UTF_8))));

    LocalFileLoader loader = new LocalFileLoader(cache);
    String bytesString = readTelemetriesFromDiskToString(loader);
    assertThat(bytesString).isEqualTo(text);
  }

  @Test
  public void testWriteGzipRawByte() throws IOException {
    String text =
        "1. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore \n"
            + "2. magna aliquyam erat, sed diam voluptua. \n"
            + "3. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum \n"
            + "4. dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore\n"
            + "5. magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, \n"
            + "6. no sea takimata sanctus est Lorem ipsum dolor sit amet.";

    // gzip
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (GZIPOutputStream out = new GZIPOutputStream(byteArrayOutputStream)) {
      out.write(text.getBytes(UTF_8));
    } finally {
      byteArrayOutputStream.close();
    }

    // write gzipped bytes[] to disk
    byte[] result = byteArrayOutputStream.toByteArray();
    LocalFileCache cache = new LocalFileCache();
    LocalFileWriter writer = new LocalFileWriter(cache);
    writer.writeToDisk(singletonList(ByteBuffer.wrap(result)));

    // read gzipped byte[] from disk
    LocalFileLoader loader = new LocalFileLoader(cache);
    byte[] bytes = readTelemetriesFromDiskToBytes(loader);

    // ungzip
    ByteArrayInputStream inputStream = new ByteArrayInputStream(result);
    byte[] ungzip = new byte[bytes.length * 3];
    int read;
    try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
      read = gzipInputStream.read(ungzip, 0, ungzip.length);
    } finally {
      inputStream.close();
    }

    assertThat(new String(Arrays.copyOf(ungzip, read), UTF_8)).isEqualTo(text);
  }

  private static void verifyTelemetryName(int index, String actualName) {
    String expectedName = null;
    if (index < 6) {
      expectedName = "Metric";
    } else if (index == 6) {
      expectedName = "Statsbeat";
    } else if (index == 7) {
      expectedName = "Message";
    } else if (index == 8) {
      expectedName = "RemoteDependency";
    } else if (index == 9) {
      expectedName = "Request";
    }

    assertThat(actualName).isEqualTo(expectedName);
  }

  private static void verifyTelemetryTime(int index, String actualTime) {
    String expectedTime = null;
    if (index < 6) {
      expectedTime = "2021-06-14T17:24:28.983-0700";
    } else if (index == 6) {
      expectedTime = "2021-06-15T12:01:02.852-0700";
    } else if (index == 7) {
      expectedTime = "2021-06-15T08:36:09.569-0700";
    } else if (index == 8) {
      expectedTime = "2021-06-15T08:36:15.229-0700";
    } else if (index == 9) {
      expectedTime = "2021-06-16T12:15:50.433-0700";
    }

    assertThat(actualTime).isEqualTo(expectedTime);
  }

  private static void verifyTagsNodeSize(int index, int actualSize) {
    int expectedSize = 0;
    if (index < 8) {
      expectedSize = 3;
    } else if (index == 8) {
      expectedSize = 4;
    } else if (index == 9) {
      expectedSize = 6;
    }

    assertThat(actualSize).isEqualTo(expectedSize);
  }

  private static void verifyDataBaseType(int index, String actualBaseType) {
    String expectedBaseType = null;
    if (index < 7) {
      expectedBaseType = "MetricData";
    } else if (index == 7) {
      expectedBaseType = "MessageData";
    } else if (index == 8) {
      expectedBaseType = "RemoteDependencyData";
    } else if (index == 9) {
      expectedBaseType = "RequestData";
    }

    assertThat(actualBaseType).isEqualTo(expectedBaseType);
  }

  private static void verifyRemoteDependencyBaseData(JsonNode baseData) {
    assertThat(baseData.get("name").asText()).isEqualTo("DROP TABLE vet_specialties IF EXISTS");
    assertThat(baseData.get("id").asText()).isEqualTo("d54e451407c13ad2");
    assertThat(baseData.get("duration").asText()).isEqualTo("00:00:00.0130000");
    assertThat(baseData.get("success").asText()).isEqualTo("true");
    assertThat(baseData.get("data").asText()).isEqualTo("DROP TABLE vet_specialties IF EXISTS");
    assertThat(baseData.get("type").asText()).isEqualTo("SQL");
    assertThat(baseData.get("target").asText()).isEqualTo("b8f14b49-a2ad-4fa9-967e-c00b1d6addc4");
  }

  private static void verifyRequestBaseData(JsonNode baseData) {
    assertThat(baseData.get("id").asText()).isEqualTo("c0bfdc8f7963802c");
    assertThat(baseData.get("duration").asText()).isEqualTo("00:00:00.0210000");
    assertThat(baseData.get("responseCode").asText()).isEqualTo("304");
    assertThat(baseData.get("success").asText()).isEqualTo("true");
    assertThat(baseData.get("name").asText()).isEqualTo("GET /webjars/**");
    assertThat(baseData.get("url").asText())
        .isEqualTo("http://localhost:8080/webjars/jquery/2.2.4/jquery.min.js");
  }

  private static void verifyMetricsName(int index, String actualName) {
    String expectedName;
    switch (index) {
      case 0:
        expectedName = "jvm_threads_states";
        break;
      case 1:
        expectedName = "hikaricp_connections_max";
        break;
      case 2:
        expectedName = "process_uptime";
        break;
      case 3:
        expectedName = "jvm_memory_used";
        break;
      case 4:
        expectedName = "jvm_threads_live";
        break;
      case 5:
        expectedName = "jdbc_connections_min";
        break;
      case 6:
        expectedName = "Request Success Count";
        break;
      default:
        expectedName = null;
        break;
    }

    assertThat(actualName).isEqualTo(expectedName);
  }

  private static void verifyMetricsValue(int index, int actualValue) {
    int expectedValue;
    switch (index) {
      case 0:
        expectedValue = 3;
        break;
      case 1:
        expectedValue = 10;
        break;
      case 2:
        expectedValue = 3131610;
        break;
      case 3:
        expectedValue = 12958128;
        break;
      case 4:
        expectedValue = 150;
        break;
      case 5:
        expectedValue = 110;
        break;
      case 6:
        expectedValue = 2;
        break;
      default:
        expectedValue = 0;
        break;
    }

    assertThat(actualValue).isEqualTo(expectedValue);
  }

  private static void verifyProperties(int index, JsonNode properties) {
    switch (index) {
      case 0:
        assertThat(properties.get("state").asText()).isEqualTo("blocked");
        return;
      case 1:
        assertThat(properties.get("pool").asText()).isEqualTo("HikariPool-1");
        return;
      case 3:
        assertThat(properties.get("area").asText()).isEqualTo("nonheap");
        assertThat(properties.get("id").asText()).isEqualTo("Compressed Class Space");
        return;
      case 4:
        assertThat(properties.get("state").asText()).isEqualTo("runnable");
        return;
      case 5:
        assertThat(properties.get("name").asText()).isEqualTo("dataSource");
        return;
      case 6: // Statsbeat
        verifyStatsbeatCustomDimensions(properties);
        return;
      case 7: // Message
        assertThat(properties.get("LoggerName").asText())
            .isEqualTo("org.springframework.boot.web.embedded.tomcat.TomcatWebServer");
        assertThat(properties.get("LoggingLevel").asText()).isEqualTo("INFO");
        assertThat(properties.get("SourceType").asText()).isEqualTo("Logger");
        return;
      case 2:
        // TODO (heya) should we delete this case?
        return;
      default:
        // all good
    }
  }

  private static void verifyStatsbeatCustomDimensions(JsonNode properties) {
    assertThat(properties.get("runtimeVersion").asText()).isEqualTo("11.0.7");
    assertThat(properties.get("os").asText()).isEqualTo("Windows");
    assertThat(properties.get("language").asText()).isEqualTo("java");
    assertThat(properties.get("attach").asText()).isEqualTo("codeless");
    assertThat(properties.get("instrumentation").asText()).isEqualTo("0");
    assertThat(properties.get("cikey").asText()).isEqualTo("00000000-0000-0000-0000-0FEEDDADBEEF");
    assertThat(properties.get("version").asText()).isEqualTo("3.1.1");
    assertThat(properties.get("rp").asText()).isEqualTo("unknown");
  }

  private static String readTelemetriesFromDiskToString(LocalFileLoader localFileLoader) {
    return new String(readTelemetriesFromDiskToBytes(localFileLoader), UTF_8);
  }

  private static byte[] readTelemetriesFromDiskToBytes(LocalFileLoader localFileLoader) {
    ByteBuffer buffer = localFileLoader.loadTelemetriesFromDisk();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    return bytes;
  }
}
