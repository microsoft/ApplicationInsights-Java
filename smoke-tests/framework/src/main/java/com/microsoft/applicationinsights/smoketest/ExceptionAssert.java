// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import org.assertj.core.api.AbstractAssert;

public class ExceptionAssert extends AbstractAssert<ExceptionAssert, Envelope> {

  public ExceptionAssert(Envelope envelope) {
    super(envelope, ExceptionAssert.class);
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasExceptionTypeName(String typeName) {
    isNotNull();
    assertThat(getExceptionData().getExceptions().get(0).getTypeName()).isEqualTo(typeName);
    return this;
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasExceptionMessage(String message) {
    isNotNull();
    assertThat(getExceptionData().getExceptions().get(0).getMessage()).isEqualTo(message);
    return this;
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasSeverityLevel(SeverityLevel severityLevel) {
    isNotNull();
    assertThat(getExceptionData().getSeverityLevel()).isEqualTo(severityLevel);
    return this;
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasProperty(String key, String value) {
    isNotNull();
    assertThat(getExceptionData().getProperties()).containsEntry(key, value);
    return this;
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasPropertyKey(String key) {
    isNotNull();
    assertThat(getExceptionData().getProperties()).containsKey(key);
    return this;
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasPropertyCount(int count) {
    isNotNull();
    assertThat(getExceptionData().getProperties()).hasSize(count);
    return this;
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasTag(String key, String value) {
    isNotNull();
    assertThat(actual.getTags()).containsEntry(key, value);
    return this;
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasNoParent() {
    isNotNull();
    assertThat(actual.getTags()).doesNotContainKey("ai.operation.parentId");
    return this;
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasParent(String parentId) {
    isNotNull();
    assertThat(actual.getTags()).containsEntry("ai.operation.parentId", parentId);
    return this;
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasSampleRate(Float expectedSampleRate) {
    isNotNull();
    assertThat(actual.getSampleRate()).isEqualTo(expectedSampleRate);
    return this;
  }

  @CanIgnoreReturnValue
  public ExceptionAssert hasNoSampleRate() {
    isNotNull();
    assertThat(actual.getSampleRate()).isNull();
    return this;
  }

  private ExceptionData getExceptionData() {
    Data<?> data = (Data<?>) actual.getData();
    return (ExceptionData) data.getBaseData();
  }
}