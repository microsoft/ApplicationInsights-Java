package com.microsoft.applicationinsights.internal.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.microsoft.applicationinsights.internal.persistence.FileLoader.DEFAULT_FOlDER;
import static org.junit.Assert.*;

public class FileLoaderTests {

    private static final String TEST_FILE_NAME = "bytebuffers-1623731284786-11229676378194009365.trn";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final File PERSISTED_FILE = new File(DEFAULT_FOlDER, TEST_FILE_NAME);

    @After
    public void cleanup() {
        if(PERSISTED_FILE.exists()) {
            assertTrue(PERSISTED_FILE.delete());
        }
    }

    @Test
    public void testLoadFile() throws IOException {
        File sourceFile = new File(Resources.getResource(TEST_FILE_NAME).getPath());

        /**
         * move this file to {@link DEFAULT_FOlDER} if it doesn't exist yet.
         */
        if (!PERSISTED_FILE.exists()) {
            FileUtils.moveFile(sourceFile, PERSISTED_FILE);
        }
        assertTrue(PERSISTED_FILE.exists());

        FileLoader.get().addPersistedFilenameToMap(TEST_FILE_NAME);
        List<byte[]> byteArrayList = FileLoader.get().loadFile();
        assertNotNull(byteArrayList);
        assertTrue(byteArrayList.size() == 10);

        for (int i = 0; i < 10; i++) {
            JsonNode jsonNode = MAPPER.readTree(byteArrayList.get(i));

            // verify common properties
            assertTrue(jsonNode.size() == 7);
            assertEquals(1, jsonNode.get("ver").asInt());
            verifyTelemetryName(i, jsonNode.get("name").asText());
            verifyTelemetryTime(i, jsonNode.get("time").asText());
            assertEquals(100, jsonNode.get("sampleRate").asInt());
            assertEquals("00000000-0000-0000-0000-0FEEDDADBEEF", jsonNode.get("iKey").asText());

            // verify tags
            JsonNode tagsNode = jsonNode.get("tags");
            verifyTagsNodeSize(i, tagsNode.size());

            assertEquals("java:3.1.1", tagsNode.get("ai.internal.sdkVersion").asText());
            assertEquals("test-role-name", tagsNode.get("ai.internal.nodeName").asText());
            assertEquals("test-role-instance", tagsNode.get("ai.cloud.roleInstance").asText());
            if (i == 8) { // RemoteDependency
                assertEquals("891b332db33c65cc6497c014f02db26d", tagsNode.get("ai.operation.id").asText());
            } else if (i == 9) {
                assertEquals("0cb22c0f071802f7f314569b007c9a1e", tagsNode.get("ai.operation.id").asText());
                assertEquals("GET /webjars/**", tagsNode.get("ai.operation.name").asText());
                assertEquals("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Safari/537.36", tagsNode.get("ai.user.userAgent").asText());
            }

            // verify data
            JsonNode data = jsonNode.get("data");
            verifyDataBaseType(i, data.get("baseType").asText());

            JsonNode baseData = data.get("baseData");
            assertEquals(2, baseData.get("ver").asInt());
            JsonNode metrics = baseData.get("metrics");

            if (i < 7) { // metrics is only applicable to Metric Telemetry type
                verifyMetricsName(i, metrics.get(0).get("name").asText());
                assertEquals(0, metrics.get(0).get("kind").asInt());
                verifyMetricsValue(i, metrics.get(0).get("value").asInt());
            }

            if (i == 7) { // Message
                assertEquals("Tomcat initialized with port(s): 8080 (http)", baseData.get("message").asText());
                assertEquals("Information", baseData.get("severityLevel").asText());
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

        assertEquals(expectedName, actualName);
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

        assertEquals(expectedTime, actualTime);
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

        assertEquals(expectedSize, actualSize);
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

        assertEquals(expectedBaseType, actualBaseType);
    }

    private void verifyRemoteDependencyBaseData(JsonNode baseData) {
        assertEquals("DROP TABLE vet_specialties IF EXISTS", baseData.get("name").asText());
        assertEquals("d54e451407c13ad2", baseData.get("id").asText());
        assertEquals("00:00:00.0130000", baseData.get("duration").asText());
        assertEquals("true", baseData.get("success").asText());
        assertEquals("DROP TABLE vet_specialties IF EXISTS", baseData.get("data").asText());
        assertEquals("SQL", baseData.get("type").asText());
        assertEquals("b8f14b49-a2ad-4fa9-967e-c00b1d6addc4", baseData.get("target").asText());
    }

    private void verifyRequestBaseData(JsonNode baseData) {
        assertEquals("c0bfdc8f7963802c", baseData.get("id").asText());
        assertEquals("00:00:00.0210000", baseData.get("duration").asText());
        assertEquals("304", baseData.get("responseCode").asText());
        assertEquals("true", baseData.get("success").asText());
        assertEquals("GET /webjars/**", baseData.get("name").asText());
        assertEquals("http://localhost:8080/webjars/jquery/2.2.4/jquery.min.js", baseData.get("url").asText());
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

        assertEquals(expectedName, actualName);
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

        assertEquals(expectedValue, actualValue);
    }

    private void verifyProperties(int index, JsonNode properties) {
        switch (index) {
            case 0:
                assertEquals("blocked", properties.get("state").asText());
                return;
            case 1:
                assertEquals("HikariPool-1", properties.get("pool").asText());
                return;
            case 3:
                assertEquals("nonheap", properties.get("area").asText());
                assertEquals("Compressed Class Space", properties.get("id").asText());
                return;
            case 4:
                assertEquals("runnable", properties.get("state").asText());
                return;
            case 5:
                assertEquals("dataSource", properties.get("name").asText());
                return;
            case 6: // Statsbeat
                verifyStatsbeatCustomDimensions(properties);
                return;
            case 7: // Message
                assertEquals("org.springframework.boot.web.embedded.tomcat.TomcatWebServer", properties.get("LoggerName").asText());
                assertEquals("INFO", properties.get("LoggingLevel").asText());
                assertEquals("Logger", properties.get("SourceType").asText());
            case 2:
            default:
                return;
        }
    }

    private void verifyStatsbeatCustomDimensions(JsonNode properties) {
        assertEquals("11.0.7", properties.get("runtimeVersion").asText());
        assertEquals("Windows", properties.get("os").asText());
        assertEquals("java", properties.get("language").asText());
        assertEquals("codeless", properties.get("attach").asText());
        assertEquals("0", properties.get("instrumentation").asText());
        assertEquals("00000000-0000-0000-0000-0FEEDDADBEEF", properties.get("cikey").asText());
        assertEquals("3.1.1", properties.get("version").asText());
        assertEquals("unknown", properties.get("rp").asText());
    }
}
