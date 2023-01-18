// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.legacyheaders;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

public class DelegatingPropagator implements TextMapPropagator {

  private static final DelegatingPropagator instance = new DelegatingPropagator();

  // in Azure Functions consumption pool, we don't know at startup whether to enable or not
  private volatile TextMapPropagator delegate = TextMapPropagator.noop();

  public static DelegatingPropagator getInstance() {
    return instance;
  }

  public void setUpStandardDelegate(
      List<String> additionalPropagators, boolean legacyRequestIdPropagationEnabled) {
    List<TextMapPropagator> propagators = new ArrayList<>();

    for (String additionalPropagator : additionalPropagators) {
      switch (additionalPropagator) {
        case "b3":
          propagators.add(B3Propagator.injectingSingleHeader());
          break;
        case "b3multi":
          propagators.add(B3Propagator.injectingMultiHeaders());
          break;
        default:
          throw new IllegalStateException(
              "Unexpected additional propagator: " + additionalPropagator);
      }
    }

    // important to add AiLegacyPropagator before W3CTraceContextPropagator, so that
    // W3CTraceContextPropagator will take precedence if both sets of headers are present
    if (legacyRequestIdPropagationEnabled) {
      propagators.add(AiLegacyPropagator.getInstance());
    }

    propagators.add(W3CTraceContextPropagator.getInstance());
    propagators.add(W3CBaggagePropagator.getInstance());

    delegate = TextMapPropagator.composite(propagators);
  }

  public void reset() {
    delegate = TextMapPropagator.noop();
  }

  @Override
  public Collection<String> fields() {
    return delegate.fields();
  }

  @Override
  public <C> void inject(Context context, @Nullable C carrier, TextMapSetter<C> setter) {
    delegate.inject(context, carrier, setter);
  }

  @Override
  public <C> Context extract(Context context, @Nullable C carrier, TextMapGetter<C> getter) {
    return delegate.extract(context, carrier, getter);
  }
}
