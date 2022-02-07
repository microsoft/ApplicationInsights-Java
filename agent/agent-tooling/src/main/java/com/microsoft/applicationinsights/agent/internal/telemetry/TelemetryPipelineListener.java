package com.microsoft.applicationinsights.agent.internal.telemetry;

import static java.util.Arrays.asList;

import java.util.List;

public interface TelemetryPipelineListener {

  void onResponse(TelemetryPipelineRequest request, TelemetryPipelineResponse response);

  void onException(
      TelemetryPipelineRequest request, String errorMessage, Throwable throwable);

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
    public void onResponse(TelemetryPipelineRequest request, TelemetryPipelineResponse response) {
      for (TelemetryPipelineListener delegate : delegates) {
        delegate.onResponse(request, response);
      }
    }

    @Override
    public void onException(
        TelemetryPipelineRequest request, String errorMessage, Throwable throwable) {
      for (TelemetryPipelineListener delegate : delegates) {
        delegate.onException(request, errorMessage, throwable);
      }
    }
  }

  class NoopTelemetryPipelineListener implements TelemetryPipelineListener {

    static final TelemetryPipelineListener INSTANCE = new NoopTelemetryPipelineListener();

    @Override
    public void onResponse(TelemetryPipelineRequest request, TelemetryPipelineResponse response) {}

    @Override
    public void onException(
        TelemetryPipelineRequest request, String errorMessage, Throwable throwable) {}
  }
}
