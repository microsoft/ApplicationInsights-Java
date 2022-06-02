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

package com.azure.monitor.opentelemetry.exporter.implementation.heartbeat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.localstorage.LocalStorageTelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryItemExporter;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipeline;
import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

class HeartbeatTests {

  private TelemetryItemExporter telemetryItemExporter;

  @TempDir File tempFolder;

  @BeforeEach
  void setup() throws MalformedURLException {
    HttpPipelineBuilder pipelineBuilder =
        new HttpPipelineBuilder().httpClient(mock(HttpClient.class));
    TelemetryPipeline telemetryPipeline =
        new TelemetryPipeline(pipelineBuilder.build(), new URL("http://foo.bar"));
    telemetryItemExporter =
        new TelemetryItemExporter(
            telemetryPipeline,
            new LocalStorageTelemetryPipelineListener(tempFolder, telemetryPipeline, null, false));
  }

  @Test
  void heartBeatPayloadContainsDataByDefault() throws InterruptedException {
    // given
    HeartBeatProvider provider = new HeartBeatProvider(60, telemetryItemExporter, t -> {});

    // some of the initialization above happens in a separate thread
    Thread.sleep(500);

    // then
    MetricsData data = (MetricsData) provider.gatherData().getData().getBaseData();
    assertThat(data).isNotNull();
    assertThat(data.getProperties().size() > 0).isTrue();
  }

  @Test
  void heartBeatPayloadContainsSpecificProperties() {
    // given
    HeartBeatProvider provider = new HeartBeatProvider(60, telemetryItemExporter, t -> {});

    // then
    assertThat(provider.addHeartBeatProperty("test", "testVal", true)).isTrue();

    MetricsData data = (MetricsData) provider.gatherData().getData().getBaseData();
    assertThat(data.getProperties().get("test")).isEqualTo("testVal");
  }

  @Test
  void heartbeatMetricIsNonZeroWhenFailureConditionPresent() {
    // given
    HeartBeatProvider provider = new HeartBeatProvider(60, telemetryItemExporter, t -> {});

    // then
    assertThat(provider.addHeartBeatProperty("test", "testVal", false)).isTrue();

    MetricsData data = (MetricsData) provider.gatherData().getData().getBaseData();
    assertThat(data.getMetrics().get(0).getValue()).isEqualTo(1);
  }

  @Test
  void heartbeatMetricCountsForAllFailures() {
    // given
    HeartBeatProvider provider = new HeartBeatProvider(60, telemetryItemExporter, t -> {});

    // then
    assertThat(provider.addHeartBeatProperty("test", "testVal", false)).isTrue();
    assertThat(provider.addHeartBeatProperty("test1", "testVal1", false)).isTrue();

    MetricsData data = (MetricsData) provider.gatherData().getData().getBaseData();
    assertThat(data.getMetrics().get(0).getValue()).isEqualTo(2);
  }

  @Test
  void sentHeartbeatContainsExpectedDefaultFields() throws Exception {
    HeartBeatProvider mockProvider = Mockito.mock(HeartBeatProvider.class);
    ConcurrentMap<String, String> props = new ConcurrentHashMap<>();
    Mockito.when(
            mockProvider.addHeartBeatProperty(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
        .then(
            (Answer<Boolean>)
                invocation -> {
                  props.put(
                      invocation.getArgument(0, String.class),
                      invocation.getArgument(1, String.class));
                  return true;
                });
    DefaultHeartBeatPropertyProvider defaultProvider = new DefaultHeartBeatPropertyProvider();

    HeartbeatDefaultPayload.populateDefaultPayload(mockProvider).call();
    Field field = defaultProvider.getClass().getDeclaredField("defaultFields");
    field.setAccessible(true);
    Set<String> defaultFields = (Set<String>) field.get(defaultProvider);
    for (String fieldName : defaultFields) {
      assertThat(props.containsKey(fieldName)).isTrue();
      assertThat(props.get(fieldName).length() > 0).isTrue();
    }
  }

  @Test
  void heartBeatProviderDoesNotAllowDuplicateProperties() {
    // given
    HeartBeatProvider provider = new HeartBeatProvider(60, telemetryItemExporter, t -> {});

    // then
    provider.addHeartBeatProperty("test01", "test val", true);
    assertThat(provider.addHeartBeatProperty("test01", "test val 2", true)).isFalse();
  }

  @Test
  void cannotAddUnknownDefaultProperty() throws Exception {
    DefaultHeartBeatPropertyProvider base = new DefaultHeartBeatPropertyProvider();
    String testKey = "testKey";

    Field field = base.getClass().getDeclaredField("defaultFields");
    field.setAccessible(true);
    Set<String> defaultFields = (Set<String>) field.get(base);
    defaultFields.add(testKey);

    HeartBeatProvider provider = new HeartBeatProvider(60, telemetryItemExporter, t -> {});

    base.setDefaultPayload(provider).call();
    MetricsData data = (MetricsData) provider.gatherData().getData().getBaseData();
    assertThat(data.getProperties().containsKey("testKey")).isFalse();
  }
}
