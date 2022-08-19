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
