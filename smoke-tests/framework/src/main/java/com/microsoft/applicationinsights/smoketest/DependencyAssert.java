// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import org.assertj.core.api.AbstractAssert;

public class DependencyAssert extends AbstractAssert<DependencyAssert, Envelope> {

  public DependencyAssert(Envelope envelope) {
    super(envelope, DependencyAssert.class);
  }

  @CanIgnoreReturnValue
  public DependencyAssert hasName(String name) {
    isNotNull();
    assertThat(getDependencyData().getName()).isEqualTo(name);
    return this;
  }

  @CanIgnoreReturnValue
  public DependencyAssert hasTarget(String target) {
    isNotNull();
    assertThat(getDependencyData().getTarget()).isEqualTo(target);
    return this;
  }

  @CanIgnoreReturnValue
  public DependencyAssert hasSuccess(boolean success) {
    isNotNull();
    assertThat(getDependencyData().getSuccess()).isEqualTo(success);
    return this;
  }

  @CanIgnoreReturnValue
  public DependencyAssert hasProperty(String key, String value) {
    isNotNull();
    assertThat(getDependencyData().getProperties()).containsEntry(key, value);
    return this;
  }

  @CanIgnoreReturnValue
  public DependencyAssert hasTag(String key, String value) {
    isNotNull();
    assertThat(actual.getTags()).containsEntry(key, value);
    return this;
  }

  @CanIgnoreReturnValue
  public DependencyAssert hasNoParent() {
    isNotNull();
    assertThat(getDependencyData().getProperties().get("ai.operation.parentId")).isNull();
    return this;
  }

  @CanIgnoreReturnValue
  public DependencyAssert hasParent(String parentId) {
    isNotNull();
    assertThat(getDependencyData().getProperties().get("ai.operation.parentId")).isNull();
    return this;
  }

  private RemoteDependencyData getDependencyData() {
    Data<?> data = (Data<?>) actual.getData();
    return (RemoteDependencyData) data.getBaseData();
  }
}
