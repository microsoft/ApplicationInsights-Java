package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import com.microsoft.applicationinsights.agent.Exporter;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ImplicitContextKeyed;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LegacyHeaderSpanProcessor implements SpanProcessor {

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        LegacyIds legacyIds = LegacyIds.fromContext(parentContext);
        // need to check that it's the same span context that was extracted
        // in order to avoid adding these attributes to downstream dependencies also
        if (legacyIds != null && legacyIds.spanContext.equals(span.getSpanContext())) {
            span.setAttribute(Exporter.AI_LEGACY_PARENT_ID_KEY, legacyIds.legacyParentId);
            if (legacyIds.legacyRootId != null) {
                span.setAttribute(Exporter.AI_LEGACY_ROOT_ID_KEY, legacyIds.legacyRootId);
            }
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {}

    @Override
    public boolean isEndRequired() {
        return false;
    }

    public static class LegacyIds implements ImplicitContextKeyed {

        private static final ContextKey<LegacyIds> AI_LEGACY_IDS_KEY = ContextKey.named("ai-legacy-ids");

        private final SpanContext spanContext;
        private final String legacyParentId;
        private final @Nullable String legacyRootId;

        private static LegacyIds fromContext(Context context) {
            return context.get(LegacyIds.AI_LEGACY_IDS_KEY);
        }

        public LegacyIds(SpanContext spanContext, String legacyParentId, @Nullable String legacyRootId) {
            this.spanContext = spanContext;
            this.legacyParentId = legacyParentId;
            this.legacyRootId = legacyRootId;
        }

        @Override
        public Context storeInContext(Context context) {
            return context.with(AI_LEGACY_IDS_KEY, this);
        }
    }
}
