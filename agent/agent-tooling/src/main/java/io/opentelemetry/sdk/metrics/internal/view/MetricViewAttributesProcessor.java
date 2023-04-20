// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.sdk.metrics.internal.view;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.microsoft.applicationinsights.agent.internal.init.AiContextKeys;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
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

    // this is needed for detecting telemetry signals that will trigger pre-aggregated metrics via
    // auto instrumentations
    Span.fromContext(context).setAttribute(AiSemanticAttributes.IS_PRE_AGGREGATED, true);

    AttributesBuilder builder = Attributes.builder();
    String connectionString = context.get(AiContextKeys.CONNECTION_STRING);
    if (connectionString != null) {
      // support for connectionStringOverrides
      // and for Classic SDK's setConnectionString()
      builder.put(AiSemanticAttributes.INTERNAL_CONNECTION_STRING, connectionString);
    }
    String roleName = context.get(AiContextKeys.ROLE_NAME);
    if (roleName != null) {
      // support for roleNameOverrides and for Classic SDK's setConnectionString()
      // and Classic SDK set...
      builder.put(AiSemanticAttributes.INTERNAL_ROLE_NAME, roleName);
    }
    applyView(builder, incoming, attributeKeys);
    if (captureSynthetic) {
      builder.put(AiSemanticAttributes.IS_SYNTHETIC, UserAgents.isBot(incoming));
    }
    return builder.build();
  }

  @Override
  public boolean usesContext() {
    return true;
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
