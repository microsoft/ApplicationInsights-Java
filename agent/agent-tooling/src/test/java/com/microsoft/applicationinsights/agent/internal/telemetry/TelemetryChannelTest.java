package com.microsoft.applicationinsights.agent.internal.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.microsoft.applicationinsights.agent.internal.MockHttpResponse;
import com.microsoft.applicationinsights.agent.internal.exporter.models.DataPointType;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricDataPoint;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricsData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MonitorBase;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.httpclient.RedirectPolicy;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileCache;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileWriter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;

public class TelemetryChannelTest {
  private TelemetryChannel telemetryChannel;
  private LocalFileCache localFileCache;
  private final AtomicInteger requestCount = new AtomicInteger();

  @TempDir File tempFolder;

  @BeforeEach
  public void setup() throws MalformedURLException {
    List<HttpPipelinePolicy> policies = new ArrayList<>();
    policies.add(new RedirectPolicy());
    HttpPipelineBuilder pipelineBuilder =
        new HttpPipelineBuilder()
            .policies(policies.toArray(new HttpPipelinePolicy[0]))
            .httpClient(
                request -> {
                  // Every alternative request will be a redirect request.
                  if (requestCount.getAndIncrement() % 2 == 0) {
                    return Mono.just(new MockHttpResponse(request, 200));
                  } else {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Location", "http://foo.bar.redirect");
                    HttpHeaders httpHeaders = new HttpHeaders(headers);
                    return Mono.just(new MockHttpResponse(request, 307, httpHeaders));
                  }
                });
    localFileCache = new LocalFileCache();
    telemetryChannel =
        new TelemetryChannel(
            pipelineBuilder.build(),
            new URL("http://foo.bar"),
            new LocalFileWriter(localFileCache, tempFolder));
  }

  @Test
  public void singleIKeyTest() {
    // given
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(
        createMetricTelemetry("metric" + 1, 1, "00000000-0000-0000-0000-0FEEDDADBEEF"));

    // when
    List<CompletableResultCode> completableResultCodes = telemetryChannel.send(telemetryItems);

    // then
    for (CompletableResultCode resultCode : completableResultCodes) {
      resultCode.join(10, TimeUnit.SECONDS);
      assertThat(resultCode.isSuccess()).isEqualTo(true);
    }
  }

  @Test
  public void dualIKeyTest() {
    // given
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(
        createMetricTelemetry("metric" + 1, 1, "00000000-0000-0000-0000-0FEEDDADBEEF"));
    telemetryItems.add(
        createMetricTelemetry("metric" + 2, 2, "00000000-0000-0000-0000-0FEEDDADBEEE"));

    // when
    List<CompletableResultCode> completableResultCodes = telemetryChannel.send(telemetryItems);

    // then
    for (CompletableResultCode resultCode : completableResultCodes) {
      resultCode.join(10, TimeUnit.SECONDS);
      assertThat(resultCode.isSuccess()).isEqualTo(true);
    }
  }

  @Test
  public void singleIKeyBatchTest() {
    // given
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(
        createMetricTelemetry("metric" + 1, 1, "00000000-0000-0000-0000-0FEEDDADBEEF"));
    telemetryItems.add(
        createMetricTelemetry("metric" + 2, 2, "00000000-0000-0000-0000-0FEEDDADBEEF"));

    // when
    List<CompletableResultCode> completableResultCodes = telemetryChannel.send(telemetryItems);

    // then
    for (CompletableResultCode resultCode : completableResultCodes) {
      resultCode.join(10, TimeUnit.SECONDS);
      assertThat(resultCode.isSuccess()).isEqualTo(true);
    }
  }

  @Test
  public void dualIKeyBatchTest() {
    // given
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(
        createMetricTelemetry("metric" + 1, 1, "00000000-0000-0000-0000-0FEEDDADBEEF"));
    telemetryItems.add(
        createMetricTelemetry("metric" + 2, 2, "00000000-0000-0000-0000-0FEEDDADBEEF"));
    telemetryItems.add(
        createMetricTelemetry("metric" + 3, 3, "00000000-0000-0000-0000-0FEEDDADBEEE"));
    telemetryItems.add(
        createMetricTelemetry("metric" + 4, 4, "00000000-0000-0000-0000-0FEEDDADBEEE"));

    // when
    List<CompletableResultCode> completableResultCodes = telemetryChannel.send(telemetryItems);

    // then
    for (CompletableResultCode resultCode : completableResultCodes) {
      resultCode.join(10, TimeUnit.SECONDS);
      assertThat(resultCode.isSuccess()).isEqualTo(true);
    }
  }

  private static TelemetryItem createMetricTelemetry(String name, int value, String iKey) {
    TelemetryItem telemetry = new TelemetryItem();
    telemetry.setVersion(1);
    telemetry.setName("Metric");
    telemetry.setInstrumentationKey(iKey);
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
