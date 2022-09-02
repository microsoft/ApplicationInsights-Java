// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import static com.microsoft.applicationinsights.agent.bootstrap.diagnostics.MsgId.CUSTOM_JMX_METRIC_ERROR;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.Collection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * The class is a base class for JMX performance counters. It knows how to fetch the needed
 * information from JMX and then relies on its derived classes to send the data.
 */
public abstract class AbstractJmxPerformanceCounter implements PerformanceCounter {

  private static final Logger logger = LoggerFactory.getLogger(AbstractJmxPerformanceCounter.class);

  private final String objectName;
  private final Collection<JmxAttributeData> attributes;
  private boolean alreadyLogged = false;

  /**
   * The main method. The method will fetch the data and send it. The method will not do anything if
   * there was a major problem accessing the needed counter.
   *
   * @param telemetryClient The telemetry client to send events.
   */
  @Override
  public synchronized void report(TelemetryClient telemetryClient) {
    try {
      Map<String, Collection<Object>> result = JmxDataFetcher.fetch(objectName, attributes);

      for (Map.Entry<String, Collection<Object>> displayAndValues : result.entrySet()) {
        boolean ok = true;
        double value = 0.0;
        for (Object obj : displayAndValues.getValue()) {
          try {
            if (obj instanceof Boolean) {
              value = ((Boolean) obj).booleanValue() ? 1 : 0;
            } else {
              value += Double.parseDouble(String.valueOf(obj));
            }
          } catch (RuntimeException e) {
            ok = false;
            break;
          }
        }

        if (ok) {
          try {
            send(telemetryClient, displayAndValues.getKey(), value);
          } catch (RuntimeException e) {
            try (MDC.MDCCloseable ignored = CUSTOM_JMX_METRIC_ERROR.makeActive()) {
              logger.error("Error while sending JMX data: '{}'", e.toString());
            }
            logger.trace("Error while sending JMX data", e);
          }
        }
      }
    } catch (Exception e) {
      if (!alreadyLogged) {
        try (MDC.MDCCloseable ignored = CUSTOM_JMX_METRIC_ERROR.makeActive()) {
          logger.error("Error while fetching JMX data: '{}'", e.toString());
        }
        logger.trace("Error while fetching JMX data", e);
        alreadyLogged = true;
      }
    }
  }

  protected AbstractJmxPerformanceCounter(
      String objectName, Collection<JmxAttributeData> attributes) {
    this.objectName = objectName;
    this.attributes = attributes;
  }

  protected abstract void send(TelemetryClient telemetryClient, String displayName, double value);
}
