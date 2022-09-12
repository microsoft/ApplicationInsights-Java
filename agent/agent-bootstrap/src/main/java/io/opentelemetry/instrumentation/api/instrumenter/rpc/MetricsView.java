// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

// Includes work from:
/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.rpc;

import com.microsoft.applicationinsights.agent.bootstrap.PreAggregatedStandardMetrics;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

// this is temporary, see
// https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/3962#issuecomment-906606325
@SuppressWarnings("rawtypes")
final class MetricsView {

  private static final Set<AttributeKey> alwaysInclude = buildAlwaysInclude();
  private static final Set<AttributeKey> clientView = buildClientView();
  private static final Set<AttributeKey> serverView = buildServerView();
  private static final Set<AttributeKey> serverFallbackView = buildServerFallbackView();

  private static Set<AttributeKey> buildAlwaysInclude() {
    // the list of recommended metrics attributes is from
    // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/rpc.md#attributes
    Set<AttributeKey> view = new HashSet<>();
    view.add(SemanticAttributes.RPC_SYSTEM);
    // START APPLICATION INSIGHTS MODIFICATIONS
    // view.add(SemanticAttributes.RPC_SERVICE);
    // view.add(SemanticAttributes.RPC_METHOD);
    view.add(BootstrapSemanticAttributes.CONNECTION_STRING);
    view.add(BootstrapSemanticAttributes.INSTRUMENTATION_KEY);
    view.add(BootstrapSemanticAttributes.ROLE_NAME);
    // END APPLICATION INSIGHTS MODIFICATIONS
    return view;
  }

  private static Set<AttributeKey> buildClientView() {
    // the list of rpc client metrics attributes is from
    // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/rpc.md#attributes
    Set<AttributeKey> view = new HashSet<>(alwaysInclude);
    view.add(SemanticAttributes.NET_PEER_NAME);
    view.add(SemanticAttributes.NET_PEER_PORT);
    // START APPLICATION INSIGHTS MODIFICATIONS
    // view.add(SemanticAttributes.NET_TRANSPORT);
    // END APPLICATION INSIGHTS MODIFICATIONS
    return view;
  }

  private static Set<AttributeKey> buildServerView() {
    // the list of rpc server metrics attributes is from
    // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/rpc.md#attributes
    Set<AttributeKey> view = new HashSet<>(alwaysInclude);
    // START APPLICATION INSIGHTS MODIFICATIONS
    // view.add(SemanticAttributes.NET_HOST_NAME);
    // view.add(SemanticAttributes.NET_TRANSPORT);
    // END APPLICATION INSIGHTS MODIFICATIONS
    return view;
  }

  private static Set<AttributeKey> buildServerFallbackView() {
    // the list of rpc server metrics attributes is from
    // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/rpc.md#attributes
    Set<AttributeKey> view = new HashSet<>(alwaysInclude);
    // START APPLICATION INSIGHTS MODIFICATIONS
    // view.add(SemanticAttributes.NET_HOST_IP);
    // view.add(SemanticAttributes.NET_TRANSPORT);
    // END APPLICATION INSIGHTS MODIFICATIONS
    return view;
  }

  private static <T> boolean containsAttribute(
      AttributeKey<T> key, Attributes startAttributes, Attributes endAttributes) {
    return startAttributes.get(key) != null || endAttributes.get(key) != null;
  }

  // START APPLICATION INSIGHTS MODIFICATIONS

  static Attributes applyClientView(
      Context context, Attributes startAttributes, Attributes endAttributes) {
    AttributesBuilder filtered = applyView(clientView, startAttributes, endAttributes);

    PreAggregatedStandardMetrics.applyRpcClientView(
        filtered, context, startAttributes, endAttributes);

    return filtered.build();
  }

  static Attributes applyServerView(
      Context context, Attributes startAttributes, Attributes endAttributes) {
    Set<AttributeKey> fullSet = serverView;
    if (!containsAttribute(SemanticAttributes.NET_HOST_NAME, startAttributes, endAttributes)) {
      fullSet = serverFallbackView;
    }
    AttributesBuilder filtered = applyView(fullSet, startAttributes, endAttributes);

    PreAggregatedStandardMetrics.applyRpcServerView(
        filtered, context, startAttributes, endAttributes);

    return filtered.build();
  }

  // END APPLICATION INSIGHTS MODIFICATIONS

  static AttributesBuilder applyView(
      Set<AttributeKey> view, Attributes startAttributes, Attributes endAttributes) {
    AttributesBuilder filtered = Attributes.builder();
    applyView(filtered, startAttributes, view);
    applyView(filtered, endAttributes, view);
    return filtered;
  }

  @SuppressWarnings("unchecked")
  private static void applyView(
      AttributesBuilder filtered, Attributes attributes, Set<AttributeKey> view) {
    attributes.forEach(
        (BiConsumer<AttributeKey, Object>)
            (key, value) -> {
              if (view.contains(key)) {
                filtered.put(key, value);
              }
            });
  }

  private MetricsView() {}
}
