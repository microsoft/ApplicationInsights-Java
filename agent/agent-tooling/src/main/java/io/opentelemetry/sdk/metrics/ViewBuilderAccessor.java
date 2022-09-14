// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.sdk.metrics;

import io.opentelemetry.sdk.metrics.internal.view.AttributesProcessor;

public class ViewBuilderAccessor {

  public static void add(ViewBuilder viewBuilder, AttributesProcessor attributeProcessor) {
    viewBuilder.addAttributesProcessor(attributeProcessor);
  }

  private ViewBuilderAccessor() {}
}
