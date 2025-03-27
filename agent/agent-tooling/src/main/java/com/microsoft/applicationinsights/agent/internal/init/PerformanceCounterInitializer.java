// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.PropertyHelper;
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
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceCounterInitializer {

  private static final Logger logger = LoggerFactory.getLogger(PerformanceCounterInitializer.class);

  public static void initialize(Configuration configuration) {

    PerformanceCounterContainer.INSTANCE.setCollectionFrequencyInSec(
        configuration.metricIntervalSeconds);

    if (logger.isDebugEnabled()) {
      PerformanceCounterContainer.INSTANCE.setLogAvailableJmxMetrics(configuration.jmxMetrics);
    }

    // We don't want these two to be flowing to the OTLP endpoint
    // because in the long term we will probably be deprecating these
    // in favor of the otel instrumentation runtime metrics that relay
    // the same information.
    registerCounterInContainer(
        "java.lang:type=Threading",
        "Current Thread Count",
        "ThreadCount",
        configuration.jmxMetrics);
    registerCounterInContainer(
        "java.lang:type=ClassLoading",
        "Loaded Class Count",
        "LoadedClassCount",
        configuration.jmxMetrics);

    JmxPerformanceCounterLoader.loadCustomJmxPerfCounters(configuration.jmxMetrics);

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

  private static void registerCounterInContainer(
      String objectName,
      String metricName,
      String attribute,
      List<Configuration.JmxMetric> jmxMetricsList) {
    if (!isMetricInConfig(objectName, attribute, jmxMetricsList)) {
      JmxAttributeData attributeData = new JmxAttributeData(metricName, attribute);
      JmxMetricPerformanceCounter jmxPerfCounter =
          new JmxMetricPerformanceCounter(objectName, Arrays.asList(attributeData));
      PerformanceCounterContainer.INSTANCE.register(jmxPerfCounter);
    }
  }

  private static boolean isMetricInConfig(
      String objectName, String attribute, List<Configuration.JmxMetric> jmxMetricsList) {
    for (Configuration.JmxMetric metric : jmxMetricsList) {
      if (metric.objectName.equals(objectName) && metric.attribute.equals(attribute)) {
        return true;
      }
    }
    return false;
  }

  private PerformanceCounterInitializer() {}
}
