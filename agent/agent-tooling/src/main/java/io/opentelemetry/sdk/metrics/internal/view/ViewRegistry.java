// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.sdk.metrics.internal.view;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.ViewBuilder;
import io.opentelemetry.sdk.metrics.ViewBuilderAccessor;
import java.util.Set;

public class ViewRegistry {

  public static void registerViews(SdkMeterProviderBuilder builder) {
    for (MetricView view : MetricView.values()) {
      registerView(
          builder, view.getInstrumentName(), view.getAttributeKeys(), view.isCaptureSynthetic());
    }
  }

  private static void registerView(
      SdkMeterProviderBuilder builder,
      String instrumentName,
      Set<AttributeKey<?>> attributeKeys,
      boolean captureSynthetic) {
    ViewBuilder viewBuilder = View.builder();
    ViewBuilderAccessor.add(
        viewBuilder, new MetricViewAttributesProcessor(attributeKeys, captureSynthetic));
    builder.registerView(
        InstrumentSelector.builder().setName(instrumentName).build(), viewBuilder.build());
  }

  private ViewRegistry() {}
}
