// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.legacyheaders;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ImplicitContextKeyed;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import javax.annotation.Nullable;

public class AiLegacyHeaderSpanProcessor implements SpanProcessor {

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    LegacyIds legacyIds = LegacyIds.fromContext(parentContext);
    // need to check that the parent span is the same as the span context extracted from
    // AiLegacyPropagator, because only want to add these properties to the request span
    if (legacyIds != null
        && legacyIds.spanContext.equals(Span.fromContext(parentContext).getSpanContext())) {
      span.setAttribute(AiSemanticAttributes.LEGACY_PARENT_ID, legacyIds.legacyParentId);
      if (legacyIds.legacyRootId != null) {
        span.setAttribute(AiSemanticAttributes.LEGACY_ROOT_ID, legacyIds.legacyRootId);
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

    private static final ContextKey<LegacyIds> AI_LEGACY_IDS_KEY =
        ContextKey.named("ai-legacy-ids");

    private final SpanContext spanContext;
    private final String legacyParentId;
    @Nullable private final String legacyRootId;

    private static LegacyIds fromContext(Context context) {
      return context.get(LegacyIds.AI_LEGACY_IDS_KEY);
    }

    public LegacyIds(
        SpanContext spanContext, String legacyParentId, @Nullable String legacyRootId) {
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
