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

package com.microsoft.applicationinsights.agent.internal.init;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.exporter.Exporter;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.AiLegacyHeaderSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithLogProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.SdkTracerProviderConfigurer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AutoService(SdkTracerProviderConfigurer.class)
public class OpenTelemetryConfigurer implements SdkTracerProviderConfigurer {

  private static volatile BatchSpanProcessor batchSpanProcessor;

  public static CompletableResultCode flush() {
    if (batchSpanProcessor == null) {
      return CompletableResultCode.ofSuccess();
    }
    return batchSpanProcessor.forceFlush();
  }

  @Override
  @SuppressFBWarnings(
      value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
      justification = "this method is only called once during initialization")
  public void configure(SdkTracerProviderBuilder tracerProvider, ConfigProperties config) {
    TelemetryClient telemetryClient = TelemetryClient.getActive();
    if (telemetryClient == null) {
      // agent failed during startup
      return;
    }

    Configuration configuration = MainEntryPoint.getConfiguration();

    tracerProvider.setSampler(DelegatingSampler.getInstance());

    if (configuration.connectionString != null) {
      DelegatingPropagator.getInstance()
          .setUpStandardDelegate(configuration.preview.legacyRequestIdPropagation.enabled);
      DelegatingSampler.getInstance()
          .setDelegate(Samplers.getSampler(configuration.sampling.percentage, configuration));
    } else {
      // in Azure Functions, we configure later on, once we know user has opted in to tracing
      // (note: the default for DelegatingPropagator is to not propagate anything
      // and the default for DelegatingSampler is to not sample anything)
    }

    // operation name span processor is only applied on span start, so doesn't need to be chained
    // with the batch span processor
    tracerProvider.addSpanProcessor(new AiOperationNameSpanProcessor());
    // inherited attributes span processor is only applied on span start, so doesn't need to be
    // chained with the batch span processor
    tracerProvider.addSpanProcessor(
        new InheritedAttributesSpanProcessor(configuration.preview.inheritedAttributes));
    // legacy span processor is only applied on span start, so doesn't need to be chained with the
    // batch span processor
    // it is used to pass legacy attributes from the context (extracted by the AiLegacyPropagator)
    // to the span attributes (since there is no way to update attributes on span directly from
    // propagator)
    if (configuration.preview.legacyRequestIdPropagation.enabled) {
      tracerProvider.addSpanProcessor(new AiLegacyHeaderSpanProcessor());
    }

    String tracesExporter = config.getString("otel.traces.exporter");
    if ("none".equals(tracesExporter)) {
      batchSpanProcessor = createExporter(configuration);
      tracerProvider.addSpanProcessor(batchSpanProcessor);
    }
  }

  private static BatchSpanProcessor createExporter(Configuration configuration) {
    List<ProcessorConfig> processors =
        configuration.preview.processors.stream()
            .filter(processor -> processor.type != Configuration.ProcessorType.METRIC_FILTER)
            .collect(Collectors.toCollection(ArrayList::new));
    // Reversing the order of processors before passing it to SpanProcessor
    Collections.reverse(processors);

    SpanExporter currExporter = new Exporter(TelemetryClient.getActive());

    // NOTE if changing the span processor to something async, flush it in the shutdown hook before
    // flushing TelemetryClient
    if (!processors.isEmpty()) {
      for (ProcessorConfig processorConfig : processors) {
        switch (processorConfig.type) {
          case ATTRIBUTE:
            currExporter = new ExporterWithAttributeProcessor(processorConfig, currExporter);
            break;
          case SPAN:
            currExporter = new ExporterWithSpanProcessor(processorConfig, currExporter);
            break;
          case LOG:
            currExporter = new ExporterWithLogProcessor(processorConfig, currExporter);
            break;
          default:
            throw new IllegalStateException(
                "Not an expected ProcessorType: " + processorConfig.type);
        }
      }
    }

    // using BatchSpanProcessor in order to get off of the application thread as soon as possible
    // using batch size 1 because need to convert to SpanData as soon as possible to grab data for
    // live metrics. the real batching is done at a lower level
    return BatchSpanProcessor.builder(currExporter).setMaxExportBatchSize(1).build();
  }
}
