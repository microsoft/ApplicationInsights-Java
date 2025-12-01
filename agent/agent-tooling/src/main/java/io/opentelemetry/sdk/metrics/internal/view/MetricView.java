// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.sdk.metrics.internal.view;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.incubating.RpcIncubatingAttributes;
import java.util.HashSet;
import java.util.Set;

enum MetricView {
  HTTP_CLIENT_VIEW("http.client.request.duration", httpClientDurationAttributeKeys(), false),
  HTTP_SERVER_VIEW("http.server.request.duration", httpServerDurationAttributeKeys(), true),
  RPC_CLIENT_VIEW("rpc.client.duration", rpcClientDurationAttributeKeys(), false),
  RPC_SERVER_VIEW("rpc.server.duration", rpcServerDurationAttributeKeys(), false);

  private final String instrumentName;

  @SuppressWarnings(
      "ImmutableEnumChecker") // mutable enum state is intentional and properly synchronized
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
    view.add(HttpAttributes.HTTP_RESPONSE_STATUS_CODE);
    view.add(ServerAttributes.SERVER_ADDRESS);
    view.add(ServerAttributes.SERVER_PORT);
    return view;
  }

  private static Set<AttributeKey<?>> httpServerDurationAttributeKeys() {
    Set<AttributeKey<?>> view = new HashSet<>(1);
    view.add(HttpAttributes.HTTP_RESPONSE_STATUS_CODE);
    return view;
  }

  private static Set<AttributeKey<?>> rpcClientDurationAttributeKeys() {
    Set<AttributeKey<?>> view = new HashSet<>(3);
    view.add(RpcIncubatingAttributes.RPC_SYSTEM);
    view.add(ServerAttributes.SERVER_ADDRESS);
    view.add(ServerAttributes.SERVER_PORT);
    return view;
  }

  private static Set<AttributeKey<?>> rpcServerDurationAttributeKeys() {
    Set<AttributeKey<?>> view = new HashSet<>(1);
    view.add(RpcIncubatingAttributes.RPC_SYSTEM);
    return view;
  }
}
