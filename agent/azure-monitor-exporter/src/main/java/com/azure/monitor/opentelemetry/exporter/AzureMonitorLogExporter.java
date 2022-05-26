package com.azure.monitor.opentelemetry.exporter;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.LogDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryItemExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.export.LogExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is an implementation of OpenTelemetry {@link LogExporter} that allows different
 * logging services to export recorded data for sampled logs in their own format.
 */
public class AzureMonitorLogExporter implements LogExporter {

  private static final ClientLogger LOGGER = new ClientLogger(AzureMonitorLogExporter.class);
  private static final OperationLogger exportingLogLogger =
      new OperationLogger(AzureMonitorLogExporter.class, "Exporting log");
  private final AtomicBoolean stopped = new AtomicBoolean();
  private final LogDataMapper mapper;
  private final TelemetryItemExporter telemetryItemExporter;

  AzureMonitorLogExporter(LogDataMapper mapper, TelemetryItemExporter telemetryItemExporter) {
    this.mapper = mapper;
    this.telemetryItemExporter = telemetryItemExporter;
  }

  /** {@inheritDoc} */
  @Override
  public CompletableResultCode export(Collection<LogData> logs) {
    if (stopped.get()) {
      return CompletableResultCode.ofFailure();
    }

    List<TelemetryItem> telemetryItems = new ArrayList<>();
    for (LogData log : logs) {
      LOGGER.verbose("exporting log: {}", log);
      try {
        mapper.map(log, telemetryItems::add);
        exportingLogLogger.recordSuccess();
      } catch (Throwable t) {
        exportingLogLogger.recordFailure(t.getMessage(), t);
        return CompletableResultCode.ofFailure();
      }
    }

    return telemetryItemExporter.send(telemetryItems);
  }

  /** {@inheritDoc} */
  @Override
  public CompletableResultCode flush() {
    return telemetryItemExporter.flush();
  }

  /** {@inheritDoc} */
  @Override
  public CompletableResultCode shutdown() {
    stopped.set(true);
    return telemetryItemExporter.shutdown();
  }
}
