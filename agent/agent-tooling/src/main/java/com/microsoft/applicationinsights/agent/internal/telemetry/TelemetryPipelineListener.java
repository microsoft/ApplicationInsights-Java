package com.microsoft.applicationinsights.agent.internal.telemetry;

import static java.util.Arrays.asList;

import java.nio.ByteBuffer;
import java.util.List;

public interface TelemetryPipelineListener {

  void onResponse(
      int responseCode,
      String responseBody,
      List<ByteBuffer> requestBody,
      String instrumentationKey);

  void onError(
      String reason, Throwable error, List<ByteBuffer> requestBody, String instrumentationKey);

  static TelemetryPipelineListener composite(TelemetryPipelineListener... delegates) {
    return new CompositeTelemetryPipelineListener(asList(delegates));
  }

  static TelemetryPipelineListener noop() {
    return NoopTelemetryPipelineListener.INSTANCE;
  }

  class CompositeTelemetryPipelineListener implements TelemetryPipelineListener {

    private final List<TelemetryPipelineListener> delegates;

    public CompositeTelemetryPipelineListener(List<TelemetryPipelineListener> delegates) {
      this.delegates = delegates;
    }

    @Override
    public void onResponse(
        int responseCode,
        String responseBody,
        List<ByteBuffer> requestBody,
        String instrumentationKey) {
      for (TelemetryPipelineListener delegate : delegates) {
        delegate.onResponse(responseCode, responseBody, requestBody, instrumentationKey);
      }
    }

    @Override
    public void onError(
        String reason, Throwable error, List<ByteBuffer> requestBody, String instrumentationKey) {
      for (TelemetryPipelineListener delegate : delegates) {
        delegate.onError(reason, error, requestBody, instrumentationKey);
      }
    }
  }

  class NoopTelemetryPipelineListener implements TelemetryPipelineListener {

    static final TelemetryPipelineListener INSTANCE = new NoopTelemetryPipelineListener();

    @Override
    public void onResponse(
        int responseCode,
        String responseBody,
        List<ByteBuffer> requestBody,
        String instrumentationKey) {}

    @Override
    public void onError(
        String reason, Throwable error, List<ByteBuffer> requestBody, String instrumentationKey) {}
  }
}
