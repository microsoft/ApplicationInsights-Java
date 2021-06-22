package com.microsoft.applicationinsights.internal.persistence;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorBase;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.TelemetryChannel;
import io.opentelemetry.sdk.common.CompletableResultCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.microsoft.applicationinsights.internal.persistence.PersistenceHelper.DEFAULT_FOLDER;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IntegrationTests {

    @AfterEach
    public void cleanup() {
        Queue<String> queue = LocalFileLoader.get().getPersistedFilesCache();
        String filename;
        while((filename = queue.poll()) != null) {
            File tempFile = new File(DEFAULT_FOLDER, filename);
            assertThat(tempFile.exists()).isTrue();
            assertThat(tempFile.delete()).isTrue();
        }
    }

    @Test
    public void integrationTest() throws MalformedURLException, InterruptedException {
        HttpClient mockedClient = mock(HttpClient.class);
        HttpRequest mockedRequest = mock(HttpRequest.class);
        HttpResponse mockedResponse = mock(HttpResponse.class);
        when(mockedResponse.getStatusCode()).thenReturn(500);
        when(mockedClient.send(mockedRequest)).thenReturn(Mono.just(mockedResponse));
        HttpPipelineBuilder pipelineBuilder = new HttpPipelineBuilder().httpClient(mockedClient);
        TelemetryChannel telemetryChannel = new TelemetryChannel(pipelineBuilder.build(), new URL("http://foo.bar"));
        LocalFileLoader.init(telemetryChannel);

        List<TelemetryItem> telemetryItems = new ArrayList<>();
        telemetryItems.add(createMetricTelemetry("metric" + 1, 1));

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                for (int j = 0; j < 10; j++) {
                    CompletableResultCode completableResultCode = telemetryChannel.send(telemetryItems);
                    completableResultCode.join(10, SECONDS);
                    assertThat(completableResultCode.isSuccess()).isEqualTo(false);
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
        assertThat(LocalFileLoader.get().getPersistedFilesCache().size()).isEqualTo(100);

        for (int i = 100; i > 0; i--) {
            LocalFileLoader.get().loadTelemetriesFromDisk(); // need to convert ByteBuffer back to TelemetryItem and then compare.
            assertThat(LocalFileLoader.get().getPersistedFilesCache().size()).isEqualTo(i - 1);
        }

        assertThat(LocalFileLoader.get().getPersistedFilesCache().size()).isEqualTo(0);
    }

    private static TelemetryItem createMetricTelemetry(String name, int value) {
        TelemetryItem telemetry = new TelemetryItem();
        telemetry.setVersion(1);
        telemetry.setName("Metric");
        telemetry.setInstrumentationKey("00000000-0000-0000-0000-0FEEDDADBEEF");
        Map<String, String> tags = new HashMap<>();
        tags.put("ai.internal.sdkVersion", "test_version");
        tags.put("ai.internal.nodeName", "test_role_name");
        tags.put("ai.cloud.roleInstance", "test_cloud_name");
        telemetry.setTags(tags);

        MetricsData data = new MetricsData();
        List<MetricDataPoint> dataPoints = new ArrayList<>();
        MetricDataPoint dataPoint = new MetricDataPoint();
        dataPoint.setDataPointType(DataPointType.MEASUREMENT);
        dataPoint.setName(name);
        dataPoint.setValue(value);
        dataPoint.setCount(1);
        dataPoints.add(dataPoint);

        Map<String, String> properties = new HashMap<>();
        properties.put("state", "blocked");

        data.setMetrics(dataPoints);
        data.setProperties(properties);

        MonitorBase monitorBase = new MonitorBase();
        monitorBase.setBaseType("MetricData");
        monitorBase.setBaseData(data);
        telemetry.setData(monitorBase);
        telemetry.setTime(new Date().toString());

        return telemetry;
    }
}
