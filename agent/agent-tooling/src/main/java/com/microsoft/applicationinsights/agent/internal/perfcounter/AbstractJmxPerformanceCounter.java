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

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.Collection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class is a base class for JMX performance counters. It knows how to fetch the needed
 * information from JMX and then relies on its derived classes to send the data.
 */
public abstract class AbstractJmxPerformanceCounter implements PerformanceCounter {

  private static final Logger logger = LoggerFactory.getLogger(AbstractJmxPerformanceCounter.class);

  private final String id;
  private final String objectName;
  private final Collection<JmxAttributeData> attributes;
  private boolean alreadyLogged = false;

  @Override
  public String getId() {
    return id;
  }

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
            logger.error("Error while sending JMX data: '{}'", e.toString());
            logger.trace("Error while sending JMX data", e);
          }
        }
      }
    } catch (Exception e) {
      if (!alreadyLogged) {
        logger.error("Error while fetching JMX data: '{}'", e.toString());
        logger.trace("Error while fetching JMX data", e);
        alreadyLogged = true;
      }
    }
  }

  protected AbstractJmxPerformanceCounter(
      String id, String objectName, Collection<JmxAttributeData> attributes) {
    this.id = id;
    this.objectName = objectName;
    this.attributes = attributes;
  }

  protected abstract void send(TelemetryClient telemetryClient, String displayName, double value);
}
