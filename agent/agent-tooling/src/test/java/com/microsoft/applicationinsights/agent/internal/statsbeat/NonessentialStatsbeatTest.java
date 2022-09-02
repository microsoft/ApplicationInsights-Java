// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NonessentialStatsbeatTest {

  private NonessentialStatsbeat nonessentialStatsbeat;

  @BeforeEach
  public void init() {
    nonessentialStatsbeat = new NonessentialStatsbeat();
  }

  @Test
  public void testIncrementReadFailureCount() {
    assertThat(nonessentialStatsbeat.getReadFailureCount()).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      nonessentialStatsbeat.incrementReadFailureCount();
    }
    assertThat(nonessentialStatsbeat.getReadFailureCount()).isEqualTo(100);
  }

  @Test
  public void testIncrementWriteFailureCount() {
    assertThat(nonessentialStatsbeat.getWriteFailureCount()).isEqualTo(0);
    for (int i = 0; i < 100; i++) {
      nonessentialStatsbeat.incrementWriteFailureCount();
    }
    assertThat(nonessentialStatsbeat.getWriteFailureCount()).isEqualTo(100);
  }
}
