// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.keytransaction;

import static com.microsoft.applicationinsights.agent.internal.keytransaction.KeyTransactionTraceState.getKeyTransactionStartTimes;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class KeyTransactionTraceStateTest {

  @Test
  void testNull() {
    assertThat(getKeyTransactionStartTimes((String) null)).isEmpty();
  }

  @Test
  void testEmpty() {
    assertThat(getKeyTransactionStartTimes("")).isEmpty();
  }

  @Test
  void testSingle() {
    Map<String, Long> startTimes = getKeyTransactionStartTimes("abc:123");
    assertThat(startTimes).containsOnlyKeys("abc");
    assertThat(startTimes.get("abc")).isEqualTo(123L);
  }

  @Test
  void testMultiple() {
    Map<String, Long> startTimes = getKeyTransactionStartTimes("abc:123;qrs:456;xyz:789");
    assertThat(startTimes).containsOnlyKeys("abc", "qrs", "xyz");
    assertThat(startTimes.get("abc")).isEqualTo(123L);
    assertThat(startTimes.get("qrs")).isEqualTo(456L);
    assertThat(startTimes.get("xyz")).isEqualTo(789L);
  }
}
