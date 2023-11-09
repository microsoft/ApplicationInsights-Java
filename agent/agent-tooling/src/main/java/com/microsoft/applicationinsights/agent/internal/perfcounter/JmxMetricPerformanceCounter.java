// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.Collection;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JmxMetricPerformanceCounter extends AbstractJmxPerformanceCounter {

  private static final Logger logger = LoggerFactory.getLogger(JmxMetricPerformanceCounter.class);

  public JmxMetricPerformanceCounter(String objectName, Collection<JmxAttributeData> attributes) {
    super(objectName, attributes);
  }

  @Override
  protected void send(TelemetryClient telemetryClient, String displayName, double value) {
    logger.debug("Metric JMX: displayName:{}, value:{} from send", displayName, value);

    //telemetryClient.trackAsync(telemetryClient.newMetricTelemetry(displayName, value));
    for (JmxAttributeData attribute : attributes) {
      logger.debug("Metric JMX: displayName:{}, attribute:{}, value:{} from send forloop", displayName, attribute.attribute, value);
      GlobalOpenTelemetry.getMeter("jmx")
          .gaugeBuilder(attribute.metricName)
          .buildWithCallback(observableDoubleMeasurement -> {
            logger.debug("Metric JMX: displayName:{},  attribute.metricName{}, value:{} from callback", displayName, attribute.metricName, value);
            observableDoubleMeasurement.record(value);
          });
    }
  }
}
