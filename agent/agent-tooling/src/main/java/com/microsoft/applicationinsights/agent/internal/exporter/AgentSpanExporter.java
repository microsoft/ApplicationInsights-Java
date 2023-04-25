// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.exporter;

import static com.azure.monitor.opentelemetry.exporter.implementation.utils.AzureMonitorMsgId.EXPORTER_MAPPING_ERROR;
import static com.microsoft.applicationinsights.agent.internal.exporter.ExporterUtils.shouldSample;

import com.azure.monitor.opentelemetry.exporter.implementation.SemanticAttributes;
import com.azure.monitor.opentelemetry.exporter.implementation.SpanDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulse;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingOverrides;
import com.microsoft.applicationinsights.agent.internal.telemetry.BatchItemProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AgentSpanExporter implements SpanExporter {

  private static final Logger logger = LoggerFactory.getLogger(AgentSpanExporter.class);

  private static final OperationLogger exportingSpanLogger =
      new OperationLogger(SpanDataMapper.class, "Exporting span");

  private final SpanDataMapper mapper;
  private final Consumer<TelemetryItem> telemetryItemConsumer;
  private final SamplingOverrides exceptionSamplingOverrides;

  public AgentSpanExporter(
      SpanDataMapper mapper,
      @Nullable QuickPulse quickPulse,
      BatchItemProcessor batchItemProcessor,
      List<Configuration.SamplingOverride> samplingOverrides) {
    this.mapper = mapper;
    telemetryItemConsumer =
        telemetryItem -> {
          if (quickPulse != null) {
            quickPulse.add(telemetryItem);
          }
          TelemetryObservers.INSTANCE
              .getObservers()
              .forEach(consumer -> consumer.accept(telemetryItem));
          batchItemProcessor.trackAsync(telemetryItem);
        };
    exceptionSamplingOverrides = new SamplingOverrides(samplingOverrides);
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    if (Strings.isNullOrEmpty(TelemetryClient.getActive().getInstrumentationKey())) {
      logger.debug("exporter is not active");
      // Azure Functions consumption plan
      return CompletableResultCode.ofSuccess();
    }
    for (SpanData span : spans) {
      logger.debug("exporting span: {}", span);
      for (EventData event : span.getEvents()) {
        event.getAttributes().forEach((k, v) -> logger.debug("event.attributes: {}:{}", k, v));
        if (event.getAttributes().get(SemanticAttributes.EXCEPTION_TYPE) != null
            || event.getAttributes().get(SemanticAttributes.EXCEPTION_MESSAGE) != null) {
          SpanContext parentSpanContext = span.getParentSpanContext();
          // Application Insights expects exception records to be "top-level" exceptions
          // not just any exception that bubbles up
          if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) {
            // TODO (trask) map OpenTelemetry exception to Application Insights exception better
            String stacktrace = event.getAttributes().get(SemanticAttributes.EXCEPTION_STACKTRACE);
            if (stacktrace != null) {
              Double samplingPercentage =
                  exceptionSamplingOverrides.getOverridePercentage(event.getAttributes());
              if (samplingPercentage != null
                  && !shouldSample(span.getSpanContext(), samplingPercentage)) {
                continue;
              }
              try {
                mapper.map(span, telemetryItemConsumer, stacktrace);
                exportingSpanLogger.recordSuccess();
              } catch (Throwable t) {
                exportingSpanLogger.recordFailure(t.getMessage(), t, EXPORTER_MAPPING_ERROR);
              }
            }
          }
        }
      }
    }
    // always returning success, because all error handling is performed internally
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }
}
