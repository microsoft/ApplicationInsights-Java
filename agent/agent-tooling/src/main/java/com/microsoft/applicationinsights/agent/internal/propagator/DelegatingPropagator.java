package com.microsoft.applicationinsights.agent.internal.propagator;

import java.util.Collection;
import javax.annotation.Nullable;

import com.microsoft.applicationinsights.agent.Exporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;

public class DelegatingPropagator implements TextMapPropagator {

    private static final DelegatingPropagator instance = new DelegatingPropagator();

    // in Azure Functions consumption pool, we don't know at startup whether to enable or not
    private volatile TextMapPropagator delegate = TextMapPropagator.noop();

    public static DelegatingPropagator getInstance() {
        return instance;
    }

    public void setUpStandardDelegate() {
        // TODO when should we enable baggage propagation?
        // currently using modified W3CTraceContextPropagator because "ai-internal-sp" trace state
        // shouldn't be sent over the wire (at least not yet, and not with that name)
        delegate = TextMapPropagator.composite(AiLegacyPropagator.getInstance(), new ModifiedW3CTraceContextPropagator());
    }

    @Override
    public Collection<String> fields() {
        return delegate.fields();
    }

    @Override
    public <C> void inject(Context context, @Nullable C carrier, Setter<C> setter) {
        delegate.inject(context, carrier, setter);
    }

    @Override
    public <C> Context extract(Context context, @Nullable C carrier, Getter<C> getter) {
        return delegate.extract(context, carrier, getter);
    }

    private static class ModifiedW3CTraceContextPropagator implements TextMapPropagator {

        private final TextMapPropagator delegate = W3CTraceContextPropagator.getInstance();

        @Override
        public Collection<String> fields() {
            return delegate.fields();
        }

        @Override
        public <C> void inject(Context context, @Nullable C carrier, Setter<C> setter) {
            // do not propagate sampling percentage downstream YET
            SpanContext spanContext = Span.fromContext(context).getSpanContext();
            // sampling percentage should always be present, so no need to optimize with checking if present
            TraceState traceState = spanContext.getTraceState();
            TraceState updatedTraceState;
            if (traceState.size() == 1 && traceState.get(Exporter.SAMPLING_PERCENTAGE_TRACE_STATE) != null) {
                // this is a common case, worth optimizing
                updatedTraceState = TraceState.getDefault();
            } else {
                updatedTraceState = traceState.toBuilder()
                        .remove(Exporter.SAMPLING_PERCENTAGE_TRACE_STATE)
                        .build();
            }
            SpanContext updatedSpanContext = new ModifiedSpanContext(spanContext, updatedTraceState);
            delegate.inject(Context.root().with(Span.wrap(updatedSpanContext)), carrier, setter);
        }

        @Override
        public <C> Context extract(Context context, @Nullable C carrier, Getter<C> getter) {
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
        public String getTraceIdAsHexString() {
            return delegate.getTraceIdAsHexString();
        }

        @Override
        public String getSpanIdAsHexString() {
            return delegate.getSpanIdAsHexString();
        }

        @Override
        public byte getTraceFlags() {
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
