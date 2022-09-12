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
public enum MetricView {
  HTTP_CLIENT_VIEW("http.client.duration", httpClientDurationAttributeKeys(), false),
  HTTP_SERVER_VIEW("http.server.duration", httpServerDurationAttributeKeys(), false),
  RPC_CLIENT_VIEW("rpc.client.duration", rpcClientDurationAttributeKeys(), false),
  RPC_SERVER_VIEW("rpc.server.duration", rpcServerDurationAttributeKeys(), false);

  private final String instrumentName;
  private final Set<AttributeKey> attributeKeys;
  private final boolean includeSynthetic;

  MetricView(String instrumentName, Set<AttributeKey> attributeKeys, boolean includeSynthetic) {
    this.instrumentName = instrumentName;
    this.attributeKeys = attributeKeys;
    this.includeSynthetic = includeSynthetic;
  }

  public static void registerViews(SdkMeterProviderBuilder builder) {
    for (MetricView view : MetricView.values()) {
      registerView(builder, view.instrumentName, view.attributeKeys, view.includeSynthetic);
    }
  }

  public static void registerView(
      SdkMeterProviderBuilder builder,
      String meterName,
      Set<AttributeKey> view,
      boolean includeSynthetic) {
    ViewBuilder viewBuilder = View.builder();
    ViewBuilderAccessor.add(viewBuilder, new MetricViewAttributesProcessor(view, includeSynthetic));
    builder.registerView(
        InstrumentSelector.builder().setName(meterName).build(), viewBuilder.build());
  }

  private static Set<AttributeKey> httpClientDurationAttributeKeys() {
    Set<AttributeKey> view = new HashSet<>(3);
    view.add(SemanticAttributes.HTTP_STATUS_CODE);
    view.add(SemanticAttributes.NET_PEER_NAME);
    view.add(SemanticAttributes.NET_PEER_PORT);
    return view;
  }

  private static Set<AttributeKey> httpServerDurationAttributeKeys() {
    Set<AttributeKey> view = new HashSet<>(1);
    view.add(SemanticAttributes.HTTP_STATUS_CODE);
    return view;
  }

  private static Set<AttributeKey> rpcClientDurationAttributeKeys() {
    Set<AttributeKey> view = new HashSet<>(3);
    view.add(SemanticAttributes.RPC_SYSTEM);
    view.add(SemanticAttributes.NET_PEER_NAME);
    view.add(SemanticAttributes.NET_PEER_PORT);
    return view;
  }

  private static Set<AttributeKey> rpcServerDurationAttributeKeys() {
    Set<AttributeKey> view = new HashSet<>(1);
    view.add(SemanticAttributes.RPC_SYSTEM);
    return view;
  }
}
