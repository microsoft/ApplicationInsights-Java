// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import org.assertj.core.api.AbstractAssert;

public class RequestAssert extends AbstractAssert<RequestAssert, Envelope> {

  public RequestAssert(Envelope envelope) {
    super(envelope, RequestAssert.class);
  }

  public RequestAssert hasName(String name) {
    isNotNull();
    assertThat(getRequestData().getName()).isEqualTo(name);
    return this;
  }

  @CanIgnoreReturnValue
  public RequestAssert hasSuccess(boolean success) {
    isNotNull();
    assertThat(getRequestData().getSuccess()).isEqualTo(success);
    return this;
  }

  @CanIgnoreReturnValue
  public RequestAssert hasProperty(String key, String value) {
    isNotNull();
    assertThat(getRequestData().getProperties()).containsEntry(key, value);
    return this;
  }

  @CanIgnoreReturnValue
  public RequestAssert hasTag(String key, String value) {
    isNotNull();
    assertThat(actual.getTags()).containsEntry(key, value);
    return this;
  }

  @CanIgnoreReturnValue
  public RequestAssert hasNoParent() {
    isNotNull();
    assertThat(getRequestData().getProperties().get("ai.operation.parentId")).isNull();
    return this;
  }

  @CanIgnoreReturnValue
  public RequestAssert hasParent(String parentId) {
    isNotNull();
    assertThat(getRequestData().getProperties().get("ai.operation.parentId")).isNull();
    return this;
  }

  private RequestData getRequestData() {
    Data<?> data = (Data<?>) actual.getData();
    return (RequestData) data.getBaseData();
  }
}
