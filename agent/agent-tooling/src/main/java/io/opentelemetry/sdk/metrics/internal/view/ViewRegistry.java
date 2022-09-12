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
          builder, view.getInstrumentName(), view.getAttributeKeys(), view.isIncludeSynthetic());
    }
  }

  private static void registerView(
      SdkMeterProviderBuilder builder,
      String meterName,
      Set<AttributeKey<?>> view,
      boolean includeSynthetic) {
    ViewBuilder viewBuilder = View.builder();
    ViewBuilderAccessor.add(viewBuilder, new MetricViewAttributesProcessor(view, includeSynthetic));
    builder.registerView(
        InstrumentSelector.builder().setName(meterName).build(), viewBuilder.build());
  }

  private ViewRegistry() {}
}
