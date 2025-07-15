// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import org.assertj.core.api.AbstractAssert;

public class MessageAssert extends AbstractAssert<MessageAssert, Envelope> {

  public MessageAssert(Envelope envelope) {
    super(envelope, MessageAssert.class);
  }

  @CanIgnoreReturnValue
  public MessageAssert hasMessage(String message) {
    isNotNull();
    assertThat(getMessageData().getMessage()).isEqualTo(message);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasSeverityLevel(SeverityLevel severityLevel) {
    isNotNull();
    assertThat(getMessageData().getSeverityLevel()).isEqualTo(severityLevel);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasProperty(String key, String value) {
    isNotNull();
    assertThat(getMessageData().getProperties()).containsEntry(key, value);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasProperty(String key) {
    isNotNull();
    assertThat(getMessageData().getProperties()).containsKey(key);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasPropertyCount(int count) {
    isNotNull();
    assertThat(getMessageData().getProperties()).hasSize(count);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasTag(String key, String value) {
    isNotNull();
    assertThat(actual.getTags()).containsEntry(key, value);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasNoSampleRate() {
    isNotNull();
    assertThat(actual.getSampleRate()).isNull();
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasSampleRate(Float expectedSampleRate) {
    isNotNull();
    assertThat(actual.getSampleRate()).isEqualTo(expectedSampleRate);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasParent(String parentId) {
    isNotNull();
    assertThat(actual.getTags()).containsEntry("ai.operation.parentId", parentId);
    return this;
  }

  private MessageData getMessageData() {
    Data<?> data = (Data<?>) actual.getData();
    return (MessageData) data.getBaseData();
  }
}