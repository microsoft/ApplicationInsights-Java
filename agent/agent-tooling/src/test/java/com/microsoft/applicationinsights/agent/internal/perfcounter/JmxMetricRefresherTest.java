// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JmxMetricRefresherTest {

  @Test
  void testDifference() {
    // given
    Map<String, Set<String>> map1 = new HashMap<>();
    map1.put("one", singleton("1"));
    map1.put("two", new HashSet<>(asList("2", "22")));
    map1.put("three", new HashSet<>(asList("3", "33", "333")));

    Map<String, Set<String>> map2 = new HashMap<>();
    map2.put("one", singleton("1"));
    map2.put("two", singleton("22"));

    // when
    Map<String, Set<String>> difference = JmxMetricRefresher.difference(map1, map2);

    // then
    assertThat(difference).containsOnlyKeys("two", "three");
    assertThat(difference.get("two")).containsExactly("2");
    assertThat(difference.get("three")).containsExactlyInAnyOrder("3", "33", "333");
  }

  @Test
  void test() {
    JmxMetricRefresher jmxMetricRefresher = new JmxMetricRefresher(Collections.emptyList());

    jmxMetricRefresher.refresh();
    jmxMetricRefresher.refresh();
  }

  @Test
  void shouldFindConfigurationFromAvailableJMxMetric() {

    List<Configuration.JmxMetric> jmxMetricsConfig = new ArrayList<>();
    Configuration.JmxMetric metric1 = new Configuration.JmxMetric();
    metric1.objectName = "objectName1";
    metric1.attribute = "attribute1";
    jmxMetricsConfig.add(metric1);

    Configuration.JmxMetric metric1bis = new Configuration.JmxMetric();
    metric1bis.objectName = "objectName1";
    metric1bis.attribute = "attribute1bis";
    jmxMetricsConfig.add(metric1bis);

    Configuration.JmxMetric metric2 = new Configuration.JmxMetric();
    metric2.objectName = "objectName2";
    metric2.attribute = "attribute2";
    jmxMetricsConfig.add(metric2);

    JmxMetricRefresher jmxMetricRefresher = new JmxMetricRefresher(jmxMetricsConfig);
    Map.Entry<String, Set<String>> newly =
        new AbstractMap.SimpleEntry<>(
            "objectName1", new HashSet<>(Collections.singletonList("attribute1")));

    List<Configuration.JmxMetric> result = jmxMetricRefresher.findConfigurations(newly);

    assertThat(result).hasSize(1);
    Configuration.JmxMetric jmxConfigurationFound = result.get(0);
    assertThat(jmxConfigurationFound.objectName).isEqualTo("objectName1");
    assertThat(jmxConfigurationFound.attribute).isEqualTo("attribute1");
  }
}
