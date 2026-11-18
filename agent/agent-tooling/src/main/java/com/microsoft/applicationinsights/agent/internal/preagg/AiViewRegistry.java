// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.preagg;

import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;

// the Application Insights specific attributes are added by PreAggregatedMetricsCustomizer,
// these views only limit the metric attributes to the ones used by the pre-aggregated metrics
public class AiViewRegistry {

  public static void registerViews(SdkMeterProviderBuilder builder) {
    for (MetricView view : MetricView.values()) {
      builder.registerView(
          InstrumentSelector.builder().setName(view.getInstrumentName()).build(),
          View.builder().setAttributeFilter(view.getRetainedAttributeKeys()).build());
    }
  }

  private AiViewRegistry() {}
}
