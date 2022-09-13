// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.sdk.metrics.internal.view;

import static com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes.IS_PRE_AGGREGATED;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.Set;
import java.util.function.BiConsumer;

@SuppressWarnings("rawtypes")
class MetricViewAttributesProcessor extends AttributesProcessor {

  private final Set<AttributeKey<?>> attributeKeys;
  private final boolean captureSynthetic;

  MetricViewAttributesProcessor(Set<AttributeKey<?>> attributeKeys, boolean captureSynthetic) {
    this.attributeKeys = attributeKeys;
    this.captureSynthetic = captureSynthetic;
  }

  @Override
  public Attributes process(Attributes incoming, Context context) {

    Span span = Span.fromContext(context);

    // this is needed for detecting telemetry signals that will trigger pre-aggregated metrics via
    // auto instrumentations
    span.setAttribute(AiSemanticAttributes.IS_PRE_AGGREGATED, true);

    AttributesBuilder filtered = Attributes.builder();
    applyCommon(filtered, span);
    applyView(filtered, incoming, attributeKeys);
    if (captureSynthetic) {
      filtered.put(AiSemanticAttributes.IS_SYNTHETIC, UserAgents.isBot(incoming));
    }
    return filtered.build();
  }

  @Override
  public boolean usesContext() {
    return true;
  }

  private static void applyCommon(AttributesBuilder builder, Span span) {

    // this is needed for detecting telemetry signals that will trigger pre-aggregated metrics via
    // auto instrumentations
    span.setAttribute(IS_PRE_AGGREGATED, true);

    if (!(span instanceof ReadableSpan)) {
      return;
    }

    ReadableSpan readableSpan = (ReadableSpan) span;

    String connectionString = readableSpan.getAttribute(AiSemanticAttributes.CONNECTION_STRING);
    if (connectionString != null) {
      builder.put(AiSemanticAttributes.CONNECTION_STRING, connectionString);
    } else {
      // back compat support
      String instrumentationKey =
          readableSpan.getAttribute(AiSemanticAttributes.INSTRUMENTATION_KEY);
      if (instrumentationKey != null) {
        builder.put(AiSemanticAttributes.INSTRUMENTATION_KEY, instrumentationKey);
      }
    }
    String roleName = readableSpan.getAttribute(AiSemanticAttributes.ROLE_NAME);
    if (roleName != null) {
      builder.put(AiSemanticAttributes.ROLE_NAME, roleName);
    }
  }

  @SuppressWarnings("unchecked")
  private static void applyView(
      AttributesBuilder filtered, Attributes attributes, Set<AttributeKey<?>> view) {
    attributes.forEach(
        (BiConsumer<AttributeKey, Object>)
            (key, value) -> {
              if (view.contains(key)) {
                filtered.put(key, value);
              }
            });
  }
}
