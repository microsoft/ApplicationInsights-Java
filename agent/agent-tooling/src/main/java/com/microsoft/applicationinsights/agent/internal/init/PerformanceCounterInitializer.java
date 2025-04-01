// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static com.microsoft.applicationinsights.agent.internal.diagnostics.MsgId.CUSTOM_JMX_METRIC_ERROR;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.PropertyHelper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.perfcounter.DeadLockDetectorPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.FreeMemoryPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.GcPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.JmxAttributeData;
import com.microsoft.applicationinsights.agent.internal.perfcounter.JmxDataFetcher;
import com.microsoft.applicationinsights.agent.internal.perfcounter.JmxMetricPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.JvmHeapMemoryUsedPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.OshiPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.PerformanceCounterContainer;
import com.microsoft.applicationinsights.agent.internal.perfcounter.ProcessCpuPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.perfcounter.ProcessMemoryPerformanceCounter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class PerformanceCounterInitializer {

  private static final Logger logger = LoggerFactory.getLogger(PerformanceCounterInitializer.class);
  private static final String METRIC_NAME_REGEXP = "[a-zA-Z0-9_.-/]+";
  private static final String INVALID_CHARACTER_REGEXP = "[^a-zA-Z0-9_.-/]";

  public static void initialize(Configuration configuration) {

    PerformanceCounterContainer.INSTANCE.setCollectionFrequencyInSec(
        configuration.metricIntervalSeconds);

    if (logger.isDebugEnabled()) {
      PerformanceCounterContainer.INSTANCE.setLogAvailableJmxMetrics();
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

  /**
   * The method will load the Jmx performance counters requested by the user to the system: 1. Build
   * a map where the key is the Jmx object name and the value is a list of requested attributes. 2.
   * Go through all the requested Jmx counters: a. If the object name is not in the map, add it with
   * an empty list Else get the list b. Add the attribute to the list. 3. Go through the map For
   * every entry (object name and attributes) to build a meter per attribute & for each meter
   * register a callback to report the metric value.
   */
  private static void loadCustomJmxPerfCounters(List<Configuration.JmxMetric> jmxXmlElements) {
    HashMap<String, Collection<JmxAttributeData>> data = new HashMap<>();

    // Build a map of object name to its requested attributes
    for (Configuration.JmxMetric jmxElement : jmxXmlElements) {
      Collection<JmxAttributeData> collection =
          data.computeIfAbsent(jmxElement.objectName, k -> new ArrayList<>());

      if (Strings.isNullOrEmpty(jmxElement.objectName)) {
        try (MDC.MDCCloseable ignored = CUSTOM_JMX_METRIC_ERROR.makeActive()) {
          logger.error("JMX object name is empty, will be ignored");
        }
        continue;
      }

      if (Strings.isNullOrEmpty(jmxElement.attribute)) {
        try (MDC.MDCCloseable ignored = CUSTOM_JMX_METRIC_ERROR.makeActive()) {
          logger.error("JMX attribute is empty for '{}'", jmxElement.objectName);
        }
        continue;
      }

      if (Strings.isNullOrEmpty(jmxElement.name)) {
        try (MDC.MDCCloseable ignored = CUSTOM_JMX_METRIC_ERROR.makeActive()) {
          logger.error("JMX name is empty for '{}', will be ignored", jmxElement.objectName);
        }
        continue;
      }

      collection.add(new JmxAttributeData(jmxElement.name, jmxElement.attribute));
    }

    createMeterPerAttribute(data);
  }

  // Create a meter for each attribute & declare the callback that reports the metric in the meter.
  private static void createMeterPerAttribute(
      Map<String, Collection<JmxAttributeData>> objectAndAttributesMap) {
    for (Map.Entry<String, Collection<JmxAttributeData>> entry :
        objectAndAttributesMap.entrySet()) {
      String objectName = entry.getKey();

      for (JmxAttributeData jmxAttributeData : entry.getValue()) {

        String otelMetricName;
        if (jmxAttributeData.metricName.matches(METRIC_NAME_REGEXP)) {
          otelMetricName = jmxAttributeData.metricName;
        } else {
          otelMetricName = jmxAttributeData.metricName.replaceAll(INVALID_CHARACTER_REGEXP, "_");
        }

        GlobalOpenTelemetry.getMeter("com.microsoft.applicationinsights.jmx")
            .gaugeBuilder(otelMetricName)
            .buildWithCallback(
                observableDoubleMeasurement -> {
                  calculateAndRecordValueForAttribute(
                      observableDoubleMeasurement, objectName, jmxAttributeData);
                });
      }
    }
  }

  private static void calculateAndRecordValueForAttribute(
      ObservableDoubleMeasurement observableDoubleMeasurement,
      String objectName,
      JmxAttributeData jmxAttributeData) {
    try {
      List<Object> result =
          JmxDataFetcher.fetch(
              objectName, jmxAttributeData.attribute); // should return the [val, ...] here

      if (!result.isEmpty()) {
        logger.info("Fetched JMX metrics: ", result);
      }

      logger.trace(
          "Size of the JmxDataFetcher.fetch result: {}, for objectName:{} and metricName:{}",
          result.size(),
          objectName,
          jmxAttributeData.metricName);

      boolean ok = true;
      double value = 0.0;
      for (Object obj : result) {
        try {
          if (obj instanceof Boolean) {
            value = ((Boolean) obj).booleanValue() ? 1 : 0;
          } else {
            value += Double.parseDouble(String.valueOf(obj));
          }
        } catch (RuntimeException e) {
          logger.warn("Unexpected value from JMX: " + result, e);
          ok = false;
          break;
        }
      }
      if (ok) {
        logger.trace(
            "value {} for objectName:{} and metricName{}",
            value,
            objectName,
            jmxAttributeData.metricName);
        observableDoubleMeasurement.record(
            value,
            Attributes.of(
                AttributeKey.stringKey("applicationinsights.internal.metric_name"),
                jmxAttributeData.metricName));
      }
    } catch (Exception e) {
      try (MDC.MDCCloseable ignored = CUSTOM_JMX_METRIC_ERROR.makeActive()) {
        logger.error(
            "Failed to calculate the metric value for objectName {} and metric name {}",
            objectName,
            jmxAttributeData.metricName);
        logger.error("Exception: {}", e.toString());
      }
    }
  }

  private PerformanceCounterInitializer() {}
}
