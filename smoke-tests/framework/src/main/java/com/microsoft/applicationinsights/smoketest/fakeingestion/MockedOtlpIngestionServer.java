package com.microsoft.applicationinsights.smoketest.fakeingestion;

import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.trace.v1.Span;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Body;
import org.mockserver.model.HttpRequest;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

public class MockedOtlpIngestionServer {
  static final int EXPORTER_ENDPOINT_PORT = 4317;
  static ClientAndServer collectorServer;

  public void startServer() throws Exception {
    System.out.println("Starting fake OTLP ingestion...");
    collectorServer = startClientAndServer(EXPORTER_ENDPOINT_PORT);
    collectorServer.when(request()).respond(response().withStatusCode(200));
  }

  @SuppressWarnings("SystemOut")
  public void stopServer() throws Exception {
    System.out.println("Stopping fake OTLP ingestion...");
    stopQuietly(collectorServer);
  }

  public void verify() {
    collectorServer.retrieveRecordedRequests(request());
    HttpRequest[] requests = collectorServer.retrieveRecordedRequests(request());

    // verify traces
    List<Span> spans = extractSpansFromRequests(requests);
    assertThat(spans)
        .extracting(Span::getName)
        .contains("Controller.doWork", "Controller.ping");

    // verify metrics
    List<Metric> metrics = extractMetricsFromRequests(requests);
    assertThat(metrics).extracting(Metric::getName).contains("apiCounter");
  }

  /**
   * Extract spans from http requests received by a telemetry collector.
   *
   * @param requests Request received by a http server trace collector
   * @return spans extracted from the request body
   */
  private static List<Span> extractSpansFromRequests(HttpRequest[] requests) {
    return Arrays.stream(requests)
        .map(HttpRequest::getBody)
        .flatMap(body -> Objects.requireNonNull(getExportTraceServiceRequest(body)).getResourceSpansList().stream())
        .flatMap(r -> r.getInstrumentationLibrarySpansList().stream())
        .flatMap(r -> r.getSpansList().stream())
        .collect(Collectors.toList());
  }

  @Nullable
  private static ExportTraceServiceRequest getExportTraceServiceRequest(Body body) {
    try {
      return ExportTraceServiceRequest.parseFrom(body.getRawBytes());
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }

  /**
   * Extract metrics from http requests received by a telemetry collector.
   *
   * @param requests Request received by an http server telemetry collector
   * @return metrics extracted from the request body
   */
  private static List<Metric> extractMetricsFromRequests(HttpRequest[] requests) {
    return Arrays.stream(requests)
        .map(HttpRequest::getBody)
        .flatMap(body -> Objects.requireNonNull(getExportMetricsServiceRequest(body)).getResourceMetricsList().stream())
        .flatMap(r -> r.getInstrumentationLibraryMetricsList().stream())
        .flatMap(r -> r.getMetricsList().stream())
        .collect(Collectors.toList());
  }

  @Nullable
  private static ExportMetricsServiceRequest getExportMetricsServiceRequest(Body body) {
    try {
      return ExportMetricsServiceRequest.parseFrom(body.getRawBytes());
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }
}
