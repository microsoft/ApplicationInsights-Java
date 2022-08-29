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

package com.microsoft.applicationinsights.agent.internal.exporter;

import static com.azure.monitor.opentelemetry.exporter.implementation.utils.AzureMonitorMsgId.EXPORTER_MAPPING_ERROR;

import com.azure.core.util.CoreUtils;
import com.azure.monitor.opentelemetry.exporter.implementation.LogDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulse;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.sampling.AiSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingOverrides;
import com.microsoft.applicationinsights.agent.internal.telemetry.BatchItemProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.Severity;
import io.opentelemetry.sdk.logs.export.LogExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentLogExporter implements LogExporter {

  private static final Logger logger = LoggerFactory.getLogger(AgentLogExporter.class);

  private static final OperationLogger exportingLogLogger =
      new OperationLogger(AgentLogExporter.class, "Exporting log");

  // TODO (trask) could implement this in a filtering LogExporter instead
  private volatile Severity threshold;

  private final SamplingOverrides logSamplingOverrides;
  private final SamplingOverrides exceptionSamplingOverrides;
  private final LogDataMapper mapper;
  private final Consumer<TelemetryItem> telemetryItemConsumer;

  public AgentLogExporter(
      Severity threshold,
      List<SamplingOverride> logSamplingOverrides,
      List<SamplingOverride> exceptionSamplingOverrides,
      LogDataMapper mapper,
      @Nullable QuickPulse quickPulse,
      BatchItemProcessor batchItemProcessor) {
    this.threshold = threshold;
    this.logSamplingOverrides = new SamplingOverrides(logSamplingOverrides);
    this.exceptionSamplingOverrides = new SamplingOverrides(exceptionSamplingOverrides);
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
  }

  public void setThreshold(Severity threshold) {
    this.threshold = threshold;
  }

  @Override
  public CompletableResultCode export(Collection<LogData> logs) {
    if (CoreUtils.isNullOrEmpty(TelemetryClient.getActive().getInstrumentationKey())) {
      // Azure Functions consumption plan
      logger.debug("Instrumentation key is null or empty. Fail to export logs.");
      return CompletableResultCode.ofFailure();
    }
    for (LogData log : logs) {
      logger.debug("exporting log: {}", log);
      try {
        int severity = log.getSeverity().getSeverityNumber();
        int threshold = this.threshold.getSeverityNumber();
        if (severity < threshold) {
          continue;
        }

        String stack = log.getAttributes().get(SemanticAttributes.EXCEPTION_STACKTRACE);

        SamplingOverrides samplingOverrides =
            stack != null ? exceptionSamplingOverrides : logSamplingOverrides;

        SpanContext spanContext = log.getSpanContext();

        boolean standaloneLog = !spanContext.isValid();
        Double samplingPercentage =
            samplingOverrides.getOverridePercentage(standaloneLog, log.getAttributes());

        if (samplingPercentage != null && !shouldSample(spanContext, samplingPercentage)) {
          continue;
        }

        if (samplingPercentage == null
            && !standaloneLog
            && !spanContext.getTraceFlags().isSampled()) {
          // if there is no sampling override, and the log is part of an unsampled trace, then don't
          // capture it
          continue;
        }

        Long itemCount = null;
        if (samplingPercentage != null) {
          // samplingPercentage cannot be 0 here
          itemCount = Math.round(100.0 / samplingPercentage);
        }

        TelemetryItem telemetryItem = mapper.map(log, stack, itemCount);
        telemetryItemConsumer.accept(telemetryItem);

        exportingLogLogger.recordSuccess();
      } catch (Throwable t) {
        exportingLogLogger.recordFailure(t.getMessage(), t, EXPORTER_MAPPING_ERROR);
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

  @SuppressFBWarnings(
      value = "SECPR", // Predictable pseudorandom number generator
      justification = "Predictable random is ok for sampling decision")
  private static boolean shouldSample(SpanContext spanContext, double percentage) {
    if (percentage == 100) {
      // optimization, no need to calculate score
      return true;
    }
    if (percentage == 0) {
      // optimization, no need to calculate score
      return false;
    }
    if (spanContext.isValid()) {
      return AiSampler.shouldRecordAndSample(spanContext.getTraceId(), percentage);
    }
    // this is a standalone log (not part of a trace), so randomly sample at the given percentage
    return ThreadLocalRandom.current().nextDouble() < percentage / 100;
  }
}
