// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static com.microsoft.applicationinsights.agent.internal.diagnostics.MsgId.CUSTOM_JMX_METRIC_ERROR;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.PropertyHelper;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class PerformanceCounterInitializer {

  private static final Logger logger = LoggerFactory.getLogger(PerformanceCounterInitializer.class);

  public static void initialize(Configuration configuration) {

    PerformanceCounterContainer.INSTANCE.setCollectionFrequencyInSec(
        configuration.metricIntervalSeconds);

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

        logger.debug("from loadCustomJmxPerfCounters- attribute: {}, objectName: {}, name: {}", jmxElement.attribute, jmxElement.objectName, jmxElement.name);

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

      logger.debug("loadcustomjmxperfcounters- data.entryset: {}", data.entrySet().toString());

      // Register each entry in the performance container
      for (Map.Entry<String, Collection<JmxAttributeData>> entry : data.entrySet()) {
        try {
          //PerformanceCounterContainer.INSTANCE.register(
              //new JmxMetricPerformanceCounter(entry.getKey(), entry.getValue()));
          String objectName = entry.getKey();
          Collection<JmxAttributeData> attributes = entry.getValue();
          for(JmxAttributeData jmxAttributeData : entry.getValue())
          {

              GlobalOpenTelemetry.meterBuilder(
                      "jmx")//.setSchemaUrl(attribute.metricName) // we want to export with the spaces name, but error is because we have spaces
                  .build()
                  .gaugeBuilder(jmxAttributeData.metricName) // replace them with underscores
                  .buildWithCallback(observableDoubleMeasurement -> {
                    logger.debug(
                        "Metric JMX from callback: objectName:{},  attribute.metricName{}, value:{} from callback",
                        objectName, jmxAttributeData.metricName);


                    try {
                      List<Object> result = JmxDataFetcher.fetch(objectName,
                          jmxAttributeData.attribute); // should return the [val] here


                    } catch (Exception e) {

                    }
                    // calculate value here




                    observableDoubleMeasurement.record(value);
                  });

          }
        } catch (RuntimeException e) {
          try (MDC.MDCCloseable ignored = CUSTOM_JMX_METRIC_ERROR.makeActive()) {
            logger.error(
                "Failed to register JMX performance counter: '{}' : '{}'",
                entry.getKey(),
                e.toString());
          }
        }
      }
    } catch (RuntimeException e) {
      try (MDC.MDCCloseable ignored = CUSTOM_JMX_METRIC_ERROR.makeActive()) {
        logger.error("Failed to register JMX performance counters: '{}'", e.toString());
      }
    }
  }

  private PerformanceCounterInitializer() {}
}
