package com.azure.monitor.opentelemetry.exporter;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulse;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulseDataCollector;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.AadAuthentication;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.EndpointProvider;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;

public class LiveMetricsSpanProcessor implements SpanProcessor {

  private static final ClientLogger LOGGER = new ClientLogger(AzureMonitorTraceExporter.class);

  private LiveMetricsSpanProcessor() {}

  public LiveMetricsSpanProcessor(
      AadAuthentication aadAuthentication,
      String roleName,
      String instrumentationKey,
      String roleInstance) {
    QuickPulse.INSTANCE.initialize(
        aadAuthentication, roleName, instrumentationKey, roleInstance, new EndpointProvider());
  }

  @Override
  public void onStart(Context context, ReadWriteSpan readWriteSpan) {
    SpanData spanData = readWriteSpan.toSpanData();
    SpanKind kind = spanData.getKind();
    String instrumentationName = spanData.getInstrumentationLibraryInfo().getName();
    String instrumentationKey = "Ikey";
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    if (kind == SpanKind.INTERNAL) {
      if (instrumentationName.startsWith("io.opentelemetry.spring-scheduling-")
          && !spanData.getParentSpanContext().isValid()) {
        // if (!span.getParentSpanContext().isValid()) {
        // TODO (trask) AI mapping: need semantic convention for determining whether to map INTERNAL
        // to request or dependency (or need clarification to use SERVER for this)
        TelemetryMappingHelper.addRequestTelemetryItem(
            spanData, instrumentationKey, telemetryItems);
      } else {
        TelemetryMappingHelper.addRemoteDependencyTelemetryItem(
            spanData, true, instrumentationKey, telemetryItems);
      }
    } else if (kind == SpanKind.CLIENT || kind == SpanKind.PRODUCER) {
      TelemetryMappingHelper.addRemoteDependencyTelemetryItem(
          spanData, false, instrumentationKey, telemetryItems);
    } else if (kind == SpanKind.CONSUMER
        && "receive".equals(spanData.getAttributes().get(SemanticAttributes.MESSAGING_OPERATION))) {
      TelemetryMappingHelper.addRemoteDependencyTelemetryItem(
          spanData, false, instrumentationKey, telemetryItems);
    } else if (kind == SpanKind.SERVER || kind == SpanKind.CONSUMER) {
      TelemetryMappingHelper.addRequestTelemetryItem(spanData, instrumentationKey, telemetryItems);
    } else {
      throw LOGGER.logExceptionAsError(new UnsupportedOperationException(kind.name()));
    }

    for (TelemetryItem telemetryItem : telemetryItems) {
      QuickPulseDataCollector.INSTANCE.add(telemetryItem);
    }
  }

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan readableSpan) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
