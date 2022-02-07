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

package com.azure.monitor.opentelemetry.exporter;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.test.TestBase;
import com.azure.core.test.TestMode;
import com.azure.core.util.Configuration;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorBase;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorDomain;
import com.azure.monitor.opentelemetry.exporter.implementation.models.RequestData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedDuration;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Base test class for Monitor Exporter client tests */
public class MonitorExporterClientTestBase extends TestBase {

  AzureMonitorExporterBuilder getClientBuilder() {
    HttpClient httpClient;
    if (getTestMode() == TestMode.RECORD || getTestMode() == TestMode.LIVE) {
      httpClient = HttpClient.createDefault();
    } else {
      httpClient = interceptorManager.getPlaybackClient();
    }

    HttpPipeline httpPipeline =
        new HttpPipelineBuilder()
            .httpClient(httpClient)
            .policies(new AzureMonitorRedirectPolicy(), interceptorManager.getRecordPolicy())
            .build();

    return new AzureMonitorExporterBuilder().pipeline(httpPipeline);
  }

  List<TelemetryItem> getAllInvalidTelemetryItems() {
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(
        createRequestData(
            "200",
            "GET /service/resource-name",
            true,
            Duration.ofMillis(100),
            OffsetDateTime.now().minusDays(10)));
    telemetryItems.add(
        createRequestData(
            "400",
            "GET /service/resource-name",
            false,
            Duration.ofMillis(50),
            OffsetDateTime.now().minusDays(10)));
    telemetryItems.add(
        createRequestData(
            "202",
            "GET /service/resource-name",
            true,
            Duration.ofMillis(125),
            OffsetDateTime.now().minusDays(10)));
    return telemetryItems;
  }

  TelemetryItem createRequestData(
      String responseCode,
      String requestName,
      boolean success,
      Duration duration,
      OffsetDateTime time) {
    MonitorDomain requestData =
        new RequestData()
            .setId(UUID.randomUUID().toString())
            .setDuration(FormattedDuration.fromNanos(duration.toNanos()))
            .setResponseCode(responseCode)
            .setSuccess(success)
            .setUrl("http://localhost:8080/")
            .setName(requestName)
            .setVersion(2);

    MonitorBase monitorBase = new MonitorBase().setBaseType("RequestData").setBaseData(requestData);

    String connectionString =
        Configuration.getGlobalConfiguration().get("APPLICATIONINSIGHTS_CONNECTION_STRING", "");

    Map<String, String> keyValues = parseConnectionString(connectionString);
    String instrumentationKey =
        keyValues.getOrDefault("InstrumentationKey", "{instrumentation-key}");

    TelemetryItem telemetryItem =
        new TelemetryItem()
            .setVersion(1)
            .setInstrumentationKey(instrumentationKey)
            .setName("test-event-name")
            .setSampleRate(100.0f)
            .setTime(time)
            .setData(monitorBase);
    return telemetryItem;
  }

  private static Map<String, String> parseConnectionString(String connectionString) {
    Objects.requireNonNull(connectionString);
    Map<String, String> keyValues = new HashMap<>();
    String[] splits = connectionString.split(";");
    for (String split : splits) {
      String[] keyValPair = split.split("=");
      if (keyValPair.length == 2) {
        keyValues.put(keyValPair[0], keyValPair[1]);
      }
    }
    return keyValues;
  }

  List<TelemetryItem> getPartiallyInvalidTelemetryItems() {
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(
        createRequestData(
            "200",
            "GET /service/resource-name",
            true,
            Duration.ofMillis(100),
            OffsetDateTime.now()));
    telemetryItems.add(
        createRequestData(
            "400",
            "GET /service/resource-name",
            false,
            Duration.ofMillis(50),
            OffsetDateTime.now().minusDays(20)));
    telemetryItems.add(
        createRequestData(
            "202",
            "GET /service/resource-name",
            true,
            Duration.ofMillis(125),
            OffsetDateTime.now()));
    return telemetryItems;
  }

  List<TelemetryItem> getValidTelemetryItems() {
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(
        createRequestData(
            "200",
            "GET /service/resource-name",
            true,
            Duration.ofMillis(100),
            OffsetDateTime.now()));
    telemetryItems.add(
        createRequestData(
            "400",
            "GET /service/resource-name",
            false,
            Duration.ofMillis(50),
            OffsetDateTime.now()));
    telemetryItems.add(
        createRequestData(
            "202",
            "GET /service/resource-name",
            true,
            Duration.ofMillis(125),
            OffsetDateTime.now()));
    return telemetryItems;
  }
}
