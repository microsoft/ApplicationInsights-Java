// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;

public class MyLogData extends DelegatingLogData {

  private final Attributes attributes;
  private final Body body;

  public MyLogData(LogRecordData delegate, Attributes attributes) {
    this(delegate, attributes, delegate.getBody());
  }

  public MyLogData(LogRecordData delegate, Attributes attributes, Body body) {
    super(delegate);
    this.attributes = attributes;
    this.body = body;
  }

  @Override
  public Body getBody() {
    return body;
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }
}
