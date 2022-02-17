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
import com.microsoft.applicationinsights.agent.internal.exporter.LoggerExporter;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithLogProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogProcessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class LoggerExporterCustomizer implements AutoConfigurationCustomizerProvider {

  @SuppressWarnings("SystemOut")
  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    System.out.println("====================================== LoggerExporterCustomizer::customize ======================================");
    Thread.dumpStack();

    autoConfiguration.addLogEmitterProviderCustomizer(
        (builder, config) -> {
          List<Configuration.ProcessorConfig> processorConfigs = reverseProcessorConfigs(MainEntryPoint.getConfiguration());
          BatchLogProcessor batchLogProcessor = createLogExporter(processorConfigs);
          if (batchLogProcessor != null) {
            builder.addLogProcessor(batchLogProcessor).build();
          }
        }
    );
  }

  @SuppressWarnings("SystemOut")
  private BatchLogProcessor createLogExporter(List<Configuration.ProcessorConfig> processorConfigs) {
    System.out.println("====================================== createLogExporter ======================================");
    LoggerExporter loggerExporter = new LoggerExporter(TelemetryClient.getActive());
    if (!processorConfigs.isEmpty()) {
      for (Configuration.ProcessorConfig processorConfig : processorConfigs) {
        if (processorConfig.type == Configuration.ProcessorType.LOG) {
          ExporterWithLogProcessor logProcessor = new ExporterWithLogProcessor(processorConfig, loggerExporter);
          return BatchLogProcessor.builder(logProcessor).setMaxExportBatchSize(1).build();
        }
      }
    }

    return null;
  }

  private List<Configuration.ProcessorConfig> reverseProcessorConfigs(Configuration configuration) {
    List<Configuration.ProcessorConfig> processors =
        configuration.preview.processors.stream()
            .filter(processor -> processor.type != Configuration.ProcessorType.METRIC_FILTER)
            .collect(Collectors.toCollection(ArrayList::new));
    // Reversing the order of processors before passing it to Span/Log processor
    Collections.reverse(processors);
    return processors;
  }
}
