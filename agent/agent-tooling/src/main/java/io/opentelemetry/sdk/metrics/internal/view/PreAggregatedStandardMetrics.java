// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.sdk.metrics.internal.view;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.ViewBuilder;
import io.opentelemetry.sdk.metrics.ViewBuilderAccessor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("rawtypes")
public class PreAggregatedStandardMetrics {

  private static final Set<AttributeKey> HTTP_CLIENT_VIEW = buildHttpClientView();
  private static final Set<AttributeKey> HTTP_SERVER_VIEW = buildHttpServerView();
  private static final Set<AttributeKey> RPC_CLIENT_VIEW = buildRpcClientView();
  private static final Set<AttributeKey> RPC_SERVER_VIEW = buildRpcServerView();

  public static void registerViews(SdkMeterProviderBuilder builder) {
    registerView(builder, "http.client.duration", HTTP_CLIENT_VIEW, false);
    registerView(builder, "http.server.duration", HTTP_SERVER_VIEW, true);
    registerView(builder, "rpc.client.duration", RPC_CLIENT_VIEW, false);
    registerView(builder, "rpc.server.duration", RPC_SERVER_VIEW, true);
  }

  public static void registerView(
      SdkMeterProviderBuilder builder,
      String meterName,
      Set<AttributeKey> view,
      boolean includeSynthetic) {
    ViewBuilder viewBuilder = View.builder();
    ViewBuilderAccessor.add(
        viewBuilder, new PreAggregatedStandardMetricsAttributesProcessor(view, includeSynthetic));
    builder.registerView(
        InstrumentSelector.builder().setName(meterName).build(), viewBuilder.build());
  }

  private static Set<AttributeKey> buildHttpClientView() {
    Set<AttributeKey> view = new HashSet<>();
    view.add(SemanticAttributes.HTTP_STATUS_CODE);
    view.add(SemanticAttributes.NET_PEER_NAME);
    view.add(SemanticAttributes.NET_PEER_PORT);
    return view;
  }

  private static Set<AttributeKey> buildHttpServerView() {
    Set<AttributeKey> view = new HashSet<>();
    view.add(SemanticAttributes.HTTP_STATUS_CODE);
    return view;
  }

  private static Set<AttributeKey> buildRpcClientView() {
    Set<AttributeKey> view = new HashSet<>();
    view.add(SemanticAttributes.RPC_SYSTEM);
    view.add(SemanticAttributes.NET_PEER_NAME);
    view.add(SemanticAttributes.NET_PEER_PORT);
    return view;
  }

  private static Set<AttributeKey> buildRpcServerView() {
    Set<AttributeKey> view = new HashSet<>();
    view.add(SemanticAttributes.RPC_SYSTEM);
    return view;
  }

  private PreAggregatedStandardMetrics() {}
}
