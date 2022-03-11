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
import com.microsoft.applicationinsights.agent.internal.exporter.AiOperationNameLogWrappingProcessor;
import com.microsoft.applicationinsights.agent.internal.exporter.LoggerExporter;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithLogProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.LogExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.export.BatchLogProcessor;
import io.opentelemetry.sdk.logs.export.LogExporter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class LoggerExporterCustomizer implements AutoConfigurationCustomizerProvider {

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    TelemetryClient telemetryClient = TelemetryClient.getActive();
    if (telemetryClient == null) {
      // agent failed during startup
      return;
    }

    autoConfiguration.addLogEmitterProviderCustomizer(
        (builder, config) -> {
          return builder.addLogProcessor(createLogExporter());
        });
  }

  private static LogProcessor createLogExporter() {
    LogExporter logExporter = new LoggerExporter(TelemetryClient.getActive());
    List<Configuration.ProcessorConfig> processorConfigs =
        getLogProcessorConfigs(MainEntryPoint.getConfiguration());
    if (!processorConfigs.isEmpty()) {
      // Reversing the order of processors before passing it Log processor
      Collections.reverse(processorConfigs);
      for (Configuration.ProcessorConfig processorConfig : processorConfigs) {
        switch (processorConfig.type) {
          case ATTRIBUTE:
            logExporter = new LogExporterWithAttributeProcessor(processorConfig, logExporter);
            break;
          case LOG:
            logExporter = new ExporterWithLogProcessor(processorConfig, logExporter);
            break;
          default:
            throw new IllegalStateException(
                "Not an expected ProcessorType: " + processorConfig.type);
        }
      }
    }

    // operation name need to be captured while on the main thread
    // before passing off to the BatchLogProcessor
    return new AiOperationNameLogWrappingProcessor(
        BatchLogProcessor.builder(logExporter).setMaxExportBatchSize(1).build());
  }

  private static List<Configuration.ProcessorConfig> getLogProcessorConfigs(
      Configuration configuration) {
    List<Configuration.ProcessorConfig> processorConfigs =
        configuration.preview.processors.stream()
            .filter(
                processor ->
                    processor.type == Configuration.ProcessorType.ATTRIBUTE
                        || processor.type == Configuration.ProcessorType.LOG)
            .collect(Collectors.toCollection(ArrayList::new));
    return processorConfigs;
  }
}
