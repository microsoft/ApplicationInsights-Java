// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.bootstrap.preagg.AiContextCustomizerHolder;
import io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizer;
import io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizerProvider;

/**
 * InstrumenterCustomizerProvider implementation that adds Application Insights context customizer
 * to all instrumenters. This replaces the need for copying the InstrumenterBuilder class.
 */
@AutoService(InstrumenterCustomizerProvider.class)
public class AiInstrumenterCustomizerProvider implements InstrumenterCustomizerProvider {

  @Override
  public void customize(InstrumenterCustomizer customizer) {
    // Add the Application Insights context customizer to all instrumenters
    // This was previously done by modifying the InstrumenterBuilder constructor
    customizer.addContextCustomizer(AiContextCustomizerHolder.getInstance());
  }
}
