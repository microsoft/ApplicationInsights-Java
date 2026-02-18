// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;

// backwards compatibility is just in case any extensions out there are using custom log processors
@SuppressWarnings("deprecation") // using deprecated Body for backwards compatibility
public class MyLogData extends DelegatingLogData {

  private final Attributes attributes;
  private final Body body;
  private final Value<?> bodyValue;

  public MyLogData(LogRecordData delegate, Attributes attributes) {
    this(delegate, attributes, delegate.getBodyValue(), delegate.getBody());
  }

  public MyLogData(LogRecordData delegate, Attributes attributes, String body) {
    this(delegate, attributes, Value.of(body), Body.string(body));
  }

  private MyLogData(LogRecordData delegate, Attributes attributes, Value<?> bodyValue, Body body) {
    super(delegate);
    this.attributes = attributes;
    this.body = body;
    this.bodyValue = bodyValue;
  }

  @Override
  public Value<?> getBodyValue() {
    return bodyValue;
  }

  @Override
  @Deprecated
  public Body getBody() {
    return body;
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }
}
