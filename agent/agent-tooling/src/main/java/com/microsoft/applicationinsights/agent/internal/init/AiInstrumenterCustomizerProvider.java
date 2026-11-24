// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.bootstrap.preagg.AiContextCustomizerHolder;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.preagg.PreAggregatedMetricsCustomizer;
import io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizer;
import io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizerProvider;

/**
 * InstrumenterCustomizerProvider implementation that adds Application Insights context customizer
 * to all instrumenters, and the pre-aggregated metrics customizer to http and rpc instrumenters.
 * This replaces the need for copying the InstrumenterBuilder class.
 */
@AutoService(InstrumenterCustomizerProvider.class)
public class AiInstrumenterCustomizerProvider implements InstrumenterCustomizerProvider {

  @Override
  public void customize(InstrumenterCustomizer customizer) {
    // Add the Application Insights context customizer to all instrumenters
    // This was previously done by modifying the InstrumenterBuilder constructor
    customizer.addContextCustomizer(AiContextCustomizerHolder.getInstance());

    Configuration configuration = FirstEntryPoint.getConfiguration();
    if (configuration != null && configuration.internal.preAggregatedStandardMetrics.enabled) {
      PreAggregatedMetricsCustomizer.customize(customizer);
    }
  }
}
