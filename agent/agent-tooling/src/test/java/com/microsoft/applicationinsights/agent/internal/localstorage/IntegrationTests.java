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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorBase;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryChannel;
import io.opentelemetry.sdk.common.CompletableResultCode;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

public class IntegrationTests {

  private TelemetryChannel telemetryChannel;
  private LocalFileCache localFileCache;
  private LocalFileLoader localFileLoader;

  @BeforeEach
  public void setup() throws MalformedURLException {
    HttpClient mockedClient = mock(HttpClient.class);
    HttpRequest mockedRequest = mock(HttpRequest.class);
    HttpResponse mockedResponse = mock(HttpResponse.class);
    when(mockedResponse.getStatusCode()).thenReturn(500);
    when(mockedClient.send(mockedRequest)).thenReturn(Mono.just(mockedResponse));
    HttpPipelineBuilder pipelineBuilder = new HttpPipelineBuilder().httpClient(mockedClient);
    localFileCache = new LocalFileCache();
    telemetryChannel =
        new TelemetryChannel(
            pipelineBuilder.build(),
            new URL("http://foo.bar"),
            new LocalFileWriter(localFileCache, false));
    localFileLoader = new LocalFileLoader(localFileCache, false);
  }

  @AfterEach
  public void cleanup() {
    Queue<String> queue = localFileCache.getPersistedFilesCache();
    String filename;
    File folder = PersistenceHelper.getDefaultFolder(false);
    while ((filename = queue.poll()) != null) {
      File tempFile = new File(folder, filename);
      assertThat(tempFile.exists()).isTrue();
      assertThat(tempFile.delete()).isTrue();
    }

    if (folder.exists()) {
      assertThat(folder.delete()).isTrue();
    }

    if (DEFAULT_FOLDER.exists()) {
      assertThat(DEFAULT_FOLDER.delete()).isTrue();
    }
  }

  @Test
  public void integrationTest() throws InterruptedException {
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(createMetricTelemetry("metric" + 1, 1));

    ExecutorService executorService = Executors.newFixedThreadPool(10);
    for (int i = 0; i < 10; i++) {
      executorService.execute(
          () -> {
            for (int j = 0; j < 10; j++) {
              CompletableResultCode completableResultCode = telemetryChannel.send(telemetryItems);
              completableResultCode.join(10, SECONDS);
              assertThat(completableResultCode.isSuccess()).isEqualTo(false);
            }
          });
    }

    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.MINUTES);
    assertThat(localFileCache.getPersistedFilesCache().size()).isEqualTo(100);

    for (int i = 100; i > 0; i--) {
      localFileLoader
          .loadTelemetriesFromDisk(); // need to convert ByteBuffer back to TelemetryItem and then
      // compare.
      assertThat(localFileCache.getPersistedFilesCache().size()).isEqualTo(i - 1);
    }

    assertThat(localFileCache.getPersistedFilesCache().size()).isEqualTo(0);
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
