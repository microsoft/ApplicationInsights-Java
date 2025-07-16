// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.HashMap;
import java.util.Map;
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
  public MessageAssert hasPropertyKey(String key) {
    isNotNull();
    assertThat(getMessageData().getProperties()).containsKey(key);
    return this;
  }

  @SafeVarargs
  @CanIgnoreReturnValue
  public final MessageAssert hasPropertiesExactly(Map.Entry<String, String>... entries) {
    Map<String, String> map = new HashMap<>();
    for (Map.Entry<String, String> entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }

    isNotNull();
    MessageData messageData = getMessageData();
    assertThat(messageData.getProperties()).containsExactlyInAnyOrderEntriesOf(map);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasPropertiesSize(int expectedSize) {
    isNotNull();
    assertThat(getMessageData().getProperties()).hasSize(expectedSize);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasTag(String key, String value) {
    isNotNull();
    assertThat(actual.getTags()).containsEntry(key, value);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasNoParent() {
    isNotNull();
    assertThat(actual.getTags()).doesNotContainKey("ai.operation.parentId");
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasParent(String parentId) {
    isNotNull();
    assertThat(actual.getTags()).containsEntry("ai.operation.parentId", parentId);
    return this;
  }

  @CanIgnoreReturnValue
  public MessageAssert hasNoSampleRate() {
    isNotNull();
    assertThat(actual.getSampleRate()).isNull();
    return this;
  }

  private MessageData getMessageData() {
    Data<?> data = (Data<?>) actual.getData();
    return (MessageData) data.getBaseData();
  }
}
