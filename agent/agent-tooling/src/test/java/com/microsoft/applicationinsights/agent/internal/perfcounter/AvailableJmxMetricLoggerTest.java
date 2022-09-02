// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AvailableJmxMetricLoggerTest {

  @Test
  public void testDifference() {
    // given
    Map<String, Set<String>> map1 = new HashMap<>();
    map1.put("one", singleton("1"));
    map1.put("two", new HashSet<>(asList("2", "22")));
    map1.put("three", new HashSet<>(asList("3", "33", "333")));

    Map<String, Set<String>> map2 = new HashMap<>();
    map2.put("one", singleton("1"));
    map2.put("two", singleton("22"));

    // when
    Map<String, Set<String>> difference = AvailableJmxMetricLogger.difference(map1, map2);

    // then
    assertThat(difference).containsOnlyKeys("two", "three");
    assertThat(difference.get("two")).containsExactly("2");
    assertThat(difference.get("three")).containsExactlyInAnyOrder("3", "33", "333");
  }

  @Test
  public void test() {
    AvailableJmxMetricLogger availableJmxMetricLogger = new AvailableJmxMetricLogger();

    availableJmxMetricLogger.logAvailableJmxMetrics();
    availableJmxMetricLogger.logAvailableJmxMetrics();
  }
}
