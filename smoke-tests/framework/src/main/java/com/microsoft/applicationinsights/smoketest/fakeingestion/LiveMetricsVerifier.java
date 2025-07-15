// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.fakeingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.DocumentIngress;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.DocumentType;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.Exception;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.MetricPoint;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.MonitoringDataPoint;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.Request;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.Trace;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LiveMetricsVerifier {

  private final List<MonitoringDataPoint> points = new CopyOnWriteArrayList<>();

  public void apply(String postBody) throws IOException {
    List<MonitoringDataPoint> dataPoints;
    try (JsonReader reader = JsonProviders.createReader(postBody)) {
      dataPoints = reader.readArray(MonitoringDataPoint::fromJson);
    }

    // Because the mock ping/posts should succeed, there should be one MonitoringDataPoint per post
    assertThat(dataPoints).hasSize(1);
    points.add(dataPoints.get(0));
  }

  public int getRequestCountFromMetric() {
    return getMetricCount("\\ApplicationInsights\\Requests/Sec");
  }

  public int getDependencyCountFromMetric() {
    return getMetricCount("\\ApplicationInsights\\Dependency Calls/Sec");
  }

  public int getRequestCount(String url) {
    int count = 0;
    for (MonitoringDataPoint point : points) {
      List<DocumentIngress> docs = point.getDocuments();
      for (DocumentIngress doc : docs) {
        if (doc.getDocumentType().equals(DocumentType.REQUEST)) {
          Request request = (Request) doc;
          if (url.equals(request.getUrl())) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public int getExceptionCount(String exceptionMessage) {
    int count = 0;
    for (MonitoringDataPoint point : points) {
      List<DocumentIngress> docs = point.getDocuments();
      for (DocumentIngress doc : docs) {
        if (doc.getDocumentType().equals(DocumentType.EXCEPTION)) {
          Exception ex = (Exception) doc;
          if (ex.getExceptionMessage().equals(exceptionMessage)) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public int getTraceCount(String traceMessage) {
    int count = 0;
    for (MonitoringDataPoint point : points) {
      List<DocumentIngress> docs = point.getDocuments();
      for (DocumentIngress doc : docs) {
        if (doc.getDocumentType().equals(DocumentType.TRACE)) {
          Trace trace = (Trace) doc;
          if (trace.getMessage().equals(traceMessage)) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public void confirmDocsAreFiltered() {
    for (MonitoringDataPoint point : points) {
      List<DocumentIngress> docs = point.getDocuments();
      for (DocumentIngress doc : docs) {
        assertThat(doc.getDocumentType()).isNotEqualTo(DocumentType.REMOTE_DEPENDENCY);
      }
    }
  }

  public void confirmPerfCountersNonZero() {
    for (MonitoringDataPoint point : points) {
      List<MetricPoint> metrics = point.getMetrics();
      for (MetricPoint metric : metrics) {
        String name = metric.getName();
        double value = metric.getValue();
        if (name.equals("\\Process\\Physical Bytes")
            || name.equals("\\% Process\\Processor Time Normalized")) {
          assertThat(value).isNotEqualTo(0);
        }
      }
    }
  }

  private int getMetricCount(String metricName) {
    int count = 0;
    for (MonitoringDataPoint point : points) {
      List<MetricPoint> metrics = point.getMetrics();
      for (MetricPoint metric : metrics) {
        String name = metric.getName();
        double value = metric.getValue();
        if (name.equals(metricName)) {
          if (Math.floor(value) != value) {
            throw new IllegalStateException("Not an integer: " + value);
          }
          count += (int) value;
        }
      }
    }
    return count;
  }
}
