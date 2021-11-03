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

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
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

    // currently using modified W3CTraceContextPropagator because "ai-internal-sp" trace state
    // shouldn't be sent over the wire (at least not yet, and not with that name)
    propagators.add(new ModifiedW3cTraceContextPropagator());
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

  private static class ModifiedW3cTraceContextPropagator implements TextMapPropagator {

    private final TextMapPropagator delegate = W3CTraceContextPropagator.getInstance();

    @Override
    public Collection<String> fields() {
      return delegate.fields();
    }

    @Override
    public <C> void inject(Context context, @Nullable C carrier, TextMapSetter<C> setter) {
      // do not propagate sampling percentage downstream YET
      SpanContext spanContext = Span.fromContext(context).getSpanContext();
      // sampling percentage should always be present, so no need to optimize with checking if
      // present
      TraceState traceState = spanContext.getTraceState();
      TraceState updatedTraceState;
      if (traceState.size() == 1
          && traceState.get(TelemetryUtil.SAMPLING_PERCENTAGE_TRACE_STATE) != null) {
        // this is a common case, worth optimizing
        updatedTraceState = TraceState.getDefault();
      } else {
        updatedTraceState =
            traceState.toBuilder().remove(TelemetryUtil.SAMPLING_PERCENTAGE_TRACE_STATE).build();
      }
      SpanContext updatedSpanContext = new ModifiedSpanContext(spanContext, updatedTraceState);
      delegate.inject(Context.root().with(Span.wrap(updatedSpanContext)), carrier, setter);
    }

    @Override
    public <C> Context extract(Context context, @Nullable C carrier, TextMapGetter<C> getter) {
      return delegate.extract(context, carrier, getter);
    }
  }

  private static class ModifiedSpanContext implements SpanContext {

    private final SpanContext delegate;
    private final TraceState traceState;

    private ModifiedSpanContext(SpanContext delegate, TraceState traceState) {
      this.delegate = delegate;
      this.traceState = traceState;
    }

    @Override
    public String getTraceId() {
      return delegate.getTraceId();
    }

    @Override
    public String getSpanId() {
      return delegate.getSpanId();
    }

    @Override
    public TraceFlags getTraceFlags() {
      return delegate.getTraceFlags();
    }

    @Override
    public TraceState getTraceState() {
      return traceState;
    }

    @Override
    public boolean isRemote() {
      return delegate.isRemote();
    }
  }
}
