package com.microsoft.applicationinsights.agent.internal.propagator;

import java.util.Collection;
import javax.annotation.Nullable;

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
        // important that W3CTraceContextPropagator is last, so it will take precedence if both sets of headers are present
        delegate = TextMapPropagator.composite(AiLegacyPropagator.getInstance(), W3CTraceContextPropagator.getInstance());
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
}
