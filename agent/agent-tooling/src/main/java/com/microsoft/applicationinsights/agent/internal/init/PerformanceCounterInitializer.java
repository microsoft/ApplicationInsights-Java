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

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.MessageId;
import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.perfcounter.DeadLockDetectorPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.FreeMemoryPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.GcPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.JmxAttributeData;
import com.microsoft.applicationinsights.agent.internal.perfcounter.JmxMetricPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.JvmHeapMemoryUsedPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.OshiPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.PerformanceCounterContainer;
import com.microsoft.applicationinsights.agent.internal.perfcounter.ProcessCpuPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.ProcessMemoryPerformanceCounter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceCounterInitializer {

  private static final Logger logger = LoggerFactory.getLogger(PerformanceCounterInitializer.class);

  public static void initialize(Configuration configuration) {

    PerformanceCounterContainer.INSTANCE.setCollectionFrequencyInSec(
        configuration.preview.metricIntervalSeconds);

    if (logger.isDebugEnabled()) {
      PerformanceCounterContainer.INSTANCE.setLogAvailableJmxMetrics();
    }

    loadCustomJmxPerfCounters(configuration.jmxMetrics);

    PerformanceCounterContainer.INSTANCE.register(
        new ProcessCpuPerformanceCounter(
            configuration.preview.useNormalizedValueForNonNormalizedCpuPercentage));
    PerformanceCounterContainer.INSTANCE.register(new ProcessMemoryPerformanceCounter());
    PerformanceCounterContainer.INSTANCE.register(new FreeMemoryPerformanceCounter());

    if (!isAgentRunningInSandboxEnvWindows()) {
      // system cpu and process disk i/o
      PerformanceCounterContainer.INSTANCE.register(new OshiPerformanceCounter());
    }

    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    if (threadBean.isSynchronizerUsageSupported()) {
      PerformanceCounterContainer.INSTANCE.register(new DeadLockDetectorPerformanceCounter());
    }
    PerformanceCounterContainer.INSTANCE.register(new JvmHeapMemoryUsedPerformanceCounter());
    PerformanceCounterContainer.INSTANCE.register(new GcPerformanceCounter());
  }

  private static boolean isAgentRunningInSandboxEnvWindows() {
    String qualifiedSdkVersion = PropertyHelper.getQualifiedSdkVersionString();
    return qualifiedSdkVersion.startsWith("awr") || qualifiedSdkVersion.startsWith("fwr");
  }

  /**
   * The method will load the Jmx performance counters requested by the user to the system: 1. Build
   * a map where the key is the Jmx object name and the value is a list of requested attributes. 2.
   * Go through all the requested Jmx counters: a. If the object name is not in the map, add it with
   * an empty list Else get the list b. Add the attribute to the list. 3. Go through the map For
   * every entry (object name and attributes) Build a {@link JmxMetricPerformanceCounter} Register
   * the Performance Counter in the {@link PerformanceCounterContainer}
   */
  private static void loadCustomJmxPerfCounters(List<Configuration.JmxMetric> jmxXmlElements) {
    try {
      HashMap<String, Collection<JmxAttributeData>> data = new HashMap<>();

      // Build a map of object name to its requested attributes
      for (Configuration.JmxMetric jmxElement : jmxXmlElements) {
        Collection<JmxAttributeData> collection =
            data.computeIfAbsent(jmxElement.objectName, k -> new ArrayList<>());

        if (Strings.isNullOrEmpty(jmxElement.objectName)) {
          logger.error(
              "JMX object name is empty, will be ignored [{}]",
              MessageId.JMX_METRIC_PERFORMANCE_COUNTER_ERROR);
          continue;
        }

        if (Strings.isNullOrEmpty(jmxElement.attribute)) {
          logger.error(
              "JMX attribute is empty for '{}', will be ignored [{}]",
              jmxElement.objectName,
              MessageId.JMX_METRIC_PERFORMANCE_COUNTER_ERROR);
          continue;
        }

        if (Strings.isNullOrEmpty(jmxElement.name)) {
          logger.error(
              "JMX name is empty for '{}', will be ignored '{}' [{}]",
              jmxElement.objectName,
              MessageId.JMX_METRIC_PERFORMANCE_COUNTER_ERROR);
          continue;
        }

        collection.add(new JmxAttributeData(jmxElement.name, jmxElement.attribute));
      }

      // Register each entry in the performance container
      for (Map.Entry<String, Collection<JmxAttributeData>> entry : data.entrySet()) {
        try {
          PerformanceCounterContainer.INSTANCE.register(
              new JmxMetricPerformanceCounter(entry.getKey(), entry.getValue()));
        } catch (RuntimeException e) {
          Object[] argumentArray = new Object[3];
          argumentArray[0] = entry.getKey();
          argumentArray[1] = e.toString();
          argumentArray[2] = MessageId.JMX_METRIC_PERFORMANCE_COUNTER_ERROR;
          logger.error("Failed to register JMX performance counter: '{}'", argumentArray);
        }
      }
    } catch (RuntimeException e) {
      logger.error(
          "Failed to register JMX performance counters: '{}' [{}]",
          e.toString(),
          MessageId.JMX_METRIC_PERFORMANCE_COUNTER_ERROR);
    }
  }

  private PerformanceCounterInitializer() {}
}
