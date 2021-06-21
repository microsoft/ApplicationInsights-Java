package com.microsoft.applicationinsights.internal.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.internal.authentication.AadAuthentication;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.DEFAULT_FOLDER;
import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.PERMANENT_FILE_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;

public class LocalFileLoaderTests {

    private static final String BYTE_BUFFERS_TEST_FILE = "read-transmission.txt";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final File PERSISTED_FILE = new File(DEFAULT_FOLDER, BYTE_BUFFERS_TEST_FILE);

    @BeforeEach
    public void setup() {
        /**
         * AadAuthentication is used by TelemetryChannel, which is used to initialize {@link LocalFileLoader}
         */
        AadAuthentication.init(null, null, null, null, null, null);
    }

    @AfterEach
    public void cleanup() {
        if(PERSISTED_FILE.exists()) {
            assertThat(PERSISTED_FILE.delete()).isTrue();
        }
    }

    @Test
    public void testSortPersistedFiles() throws InterruptedException {
        List<File> sourceList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String filename = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replaceAll("-", "") + PERMANENT_FILE_EXTENSION;
            sourceList.add(new File(DEFAULT_FOLDER, filename));
            Thread.sleep(10);
        }

        List<File> copiedSourceList = new ArrayList<>();
        copiedSourceList.addAll(sourceList);
        Collections.shuffle(copiedSourceList);

        List<File> sortedFiles = LocalFileLoader.get().sortPersistedFiles((Collection<File>) copiedSourceList);
        for (int i = 0; i < 10; i++) {
            assertThat(sortedFiles.get(i)).isEqualTo(sourceList.get(i));
        }
    }

    @Test
    public void testLoadFile() throws IOException {
        File sourceFile = new File(getClass().getClassLoader().getResource(BYTE_BUFFERS_TEST_FILE).getPath());

        /**
         * move this file to {@link DEFAULT_FOlDER} if it doesn't exist yet.
         */
        if (!PERSISTED_FILE.exists()) {
            FileUtils.moveFile(sourceFile, PERSISTED_FILE);
        }
        assertThat(PERSISTED_FILE.exists()).isTrue();

        LocalFileLoader.get().addPersistedFilenameToMap(BYTE_BUFFERS_TEST_FILE);
        byte[] bytes = LocalFileLoader.get().loadTelemetriesFromDisk();
        assertThat(bytes).isNotNull();

        String bytesString = new String(bytes);
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
                assertThat(tagsNode.get("ai.operation.id").asText()).isEqualTo("891b332db33c65cc6497c014f02db26d");
            } else if (i == 9) {
                assertThat(tagsNode.get("ai.operation.id").asText()).isEqualTo("0cb22c0f071802f7f314569b007c9a1e");
                assertThat(tagsNode.get("ai.operation.name").asText()).isEqualTo("GET /webjars/**");
                assertThat(tagsNode.get("ai.user.userAgent").asText()).isEqualTo("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Safari/537.36");
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
                assertThat(baseData.get("message").asText()).isEqualTo("Tomcat initialized with port(s): 8080 (http)");
                assertThat(baseData.get("severityLevel").asText()).isEqualTo("Information");
            }

            if (i == 8) { // RemoteDependency's baseData
                verifyRemoteDependencyBaseData(baseData);
            }

            if (i == 9) {  // Request's baseData
                verifyRequestBaseData(baseData);
            }

            // verify properties
            verifyProperties(i, baseData.get("properties"));
        }
    }

    @Test
    public void testWriteAndReadRandomText() {
        String text = "hello world";
        LocalFileWriter writer = new LocalFileWriter();
        writer.writeToDisk(text.getBytes());

        byte[] rawBytesFromDisk = LocalFileLoader.get().loadTelemetriesFromDisk();
        assertThat(new String(rawBytesFromDisk)).isEqualTo(text);
    }

    @Test
    public void testWriteGzipRawByte() throws IOException {
        String text = "hello world";

        // gzip
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(byteArrayOutputStream)) {
            out.write(text.getBytes());
        } catch (Exception ex) {
            throw ex;
        } finally {
            byteArrayOutputStream.close();
        }

        // write gzipped bytes[] to disk
        byte[] result = byteArrayOutputStream.toByteArray();
        LocalFileWriter writer = new LocalFileWriter();
        writer.writeToDisk(result);

        // read gzipped byte[] from disk
        byte[] persistedBytes = LocalFileLoader.get().loadTelemetriesFromDisk();

        // ungzip
        ByteArrayInputStream inputStream = new ByteArrayInputStream(result);
        byte[] ungzip = new byte[persistedBytes.length];
        int read = 0;
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
            read = gzipInputStream.read(ungzip, 0, ungzip.length);
        } catch (Exception ex) {
            throw ex;
        } finally {
            inputStream.close();
        }

        assertThat(new String(Arrays.copyOf(ungzip, read))).isEqualTo(text);
    }

    private void verifyTelemetryName(int index, String actualName) {
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

    private void verifyTelemetryTime(int index, String actualTime) {
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

    private void verifyTagsNodeSize(int index, int actualSize) {
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

    private void verifyDataBaseType(int index, String actualBaseType) {
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

    private void verifyRemoteDependencyBaseData(JsonNode baseData) {
        assertThat(baseData.get("name").asText()).isEqualTo("DROP TABLE vet_specialties IF EXISTS");
        assertThat(baseData.get("id").asText()).isEqualTo("d54e451407c13ad2");
        assertThat(baseData.get("duration").asText()).isEqualTo("00:00:00.0130000");
        assertThat(baseData.get("success").asText()).isEqualTo("true");
        assertThat(baseData.get("data").asText()).isEqualTo("DROP TABLE vet_specialties IF EXISTS");
        assertThat(baseData.get("type").asText()).isEqualTo("SQL");
        assertThat(baseData.get("target").asText()).isEqualTo("b8f14b49-a2ad-4fa9-967e-c00b1d6addc4");
    }

    private void verifyRequestBaseData(JsonNode baseData) {
        assertThat(baseData.get("id").asText()).isEqualTo("c0bfdc8f7963802c");
        assertThat(baseData.get("duration").asText()).isEqualTo("00:00:00.0210000");
        assertThat(baseData.get("responseCode").asText()).isEqualTo("304");
        assertThat(baseData.get("success").asText()).isEqualTo("true");
        assertThat(baseData.get("name").asText()).isEqualTo("GET /webjars/**");
        assertThat(baseData.get("url").asText()).isEqualTo("http://localhost:8080/webjars/jquery/2.2.4/jquery.min.js");
    }

    private void verifyMetricsName(int index, String actualName) {
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

    private void verifyMetricsValue(int index, int actualValue) {
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

    private void verifyProperties(int index, JsonNode properties) {
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
                assertThat(properties.get("LoggerName").asText()).isEqualTo("org.springframework.boot.web.embedded.tomcat.TomcatWebServer");
                assertThat(properties.get("LoggingLevel").asText()).isEqualTo("INFO");
                assertThat(properties.get("SourceType").asText()).isEqualTo("Logger");
            case 2:
            default:
                return;
        }
    }

    private void verifyStatsbeatCustomDimensions(JsonNode properties) {
        assertThat(properties.get("runtimeVersion").asText()).isEqualTo("11.0.7");
        assertThat(properties.get("os").asText()).isEqualTo("Windows");
        assertThat(properties.get("language").asText()).isEqualTo("java");
        assertThat(properties.get("attach").asText()).isEqualTo("codeless");
        assertThat(properties.get("instrumentation").asText()).isEqualTo("0");
        assertThat(properties.get("cikey").asText()).isEqualTo("00000000-0000-0000-0000-0FEEDDADBEEF");
        assertThat(properties.get("version").asText()).isEqualTo("3.1.1");
        assertThat(properties.get("rp").asText()).isEqualTo("unknown");
    }
}
