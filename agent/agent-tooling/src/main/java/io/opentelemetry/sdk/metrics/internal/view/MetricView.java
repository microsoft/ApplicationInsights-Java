// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.sdk.metrics.internal.view;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.HashSet;
import java.util.Set;

enum MetricView {
  HTTP_CLIENT_VIEW("http.client.duration", httpClientDurationAttributeKeys(), false),
  HTTP_SERVER_VIEW("http.server.duration", httpServerDurationAttributeKeys(), true),
  RPC_CLIENT_VIEW("rpc.client.duration", rpcClientDurationAttributeKeys(), false),
  RPC_SERVER_VIEW("rpc.server.duration", rpcServerDurationAttributeKeys(), false);

  private final String instrumentName;

  @SuppressWarnings("ImmutableEnumChecker")
  private final Set<AttributeKey<?>> attributeKeys;

  private final boolean captureSynthetic;

  MetricView(String instrumentName, Set<AttributeKey<?>> attributeKeys, boolean captureSynthetic) {
    this.instrumentName = instrumentName;
    this.attributeKeys = attributeKeys;
    this.captureSynthetic = captureSynthetic;
  }

  String getInstrumentName() {
    return instrumentName;
  }

  Set<AttributeKey<?>> getAttributeKeys() {
    return attributeKeys;
  }

  boolean isCaptureSynthetic() {
    return captureSynthetic;
  }

  private static Set<AttributeKey<?>> httpClientDurationAttributeKeys() {
    Set<AttributeKey<?>> view = new HashSet<>(3);
    view.add(SemanticAttributes.HTTP_STATUS_CODE);
    view.add(SemanticAttributes.NET_PEER_NAME);
    view.add(SemanticAttributes.NET_PEER_PORT);
    return view;
  }

  private static Set<AttributeKey<?>> httpServerDurationAttributeKeys() {
    Set<AttributeKey<?>> view = new HashSet<>(1);
    view.add(SemanticAttributes.HTTP_STATUS_CODE);
    return view;
  }

  private static Set<AttributeKey<?>> rpcClientDurationAttributeKeys() {
    Set<AttributeKey<?>> view = new HashSet<>(3);
    view.add(SemanticAttributes.RPC_SYSTEM);
    view.add(SemanticAttributes.NET_PEER_NAME);
    view.add(SemanticAttributes.NET_PEER_PORT);
    return view;
  }

  private static Set<AttributeKey<?>> rpcServerDurationAttributeKeys() {
    Set<AttributeKey<?>> view = new HashSet<>(1);
    view.add(SemanticAttributes.RPC_SYSTEM);
    return view;
  }
}
