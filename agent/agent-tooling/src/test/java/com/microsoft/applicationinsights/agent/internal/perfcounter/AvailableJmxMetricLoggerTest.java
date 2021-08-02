/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
