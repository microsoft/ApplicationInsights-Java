// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.pipelines;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.AlertPipelineTrigger;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.Aggregation;
import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import com.microsoft.applicationinsights.alerting.analysis.filter.AlertRequestFilter;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import java.lang.management.ManagementFactory;
import java.time.format.DateTimeFormatter;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains a pipeline that receives telemetry, feeds it into the analysis pipeline. */
public class SingleAlertPipeline implements AlertPipeline, AlertPipelineMXBean {

  private static final Logger logger = LoggerFactory.getLogger(SingleAlertPipeline.class);
  private static final String JMX_KEY = "com.microsoft:type=AI-alert,name=";

  private final Aggregation aggregation;
  private final Consumer<AlertBreach> alertObserver;
  private final AlertRequestFilter filter;

  private AlertConfiguration alertConfiguration;
  private AlertPipelineTrigger alertTrigger;

  public SingleAlertPipeline(
      AlertRequestFilter filter,
      Aggregation aggregation,
      Consumer<AlertBreach> alertObserver,
      AlertConfiguration alertConfiguration) {
    this.filter = filter;
    this.aggregation = aggregation;
    this.alertObserver = alertObserver;
    this.alertConfiguration = alertConfiguration;
  }

  public static SingleAlertPipeline create(
      AlertRequestFilter filter,
      Aggregation aggregation,
      AlertConfiguration alertConfiguration,
      Consumer<AlertBreach> alertObserver) {
    SingleAlertPipeline trigger =
        new SingleAlertPipeline(filter, aggregation, alertObserver, alertConfiguration);
    trigger.registerMbean();

    trigger.setAlertTrigger(aggregation, alertConfiguration, alertObserver);
    return trigger;
  }

  private void registerMbean() {
    try {
      ObjectName objectName = new ObjectName(JMX_KEY + alertConfiguration.getType().name());

      try {
        MBeanInfo existing = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(objectName);
        if (existing != null) {
          ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);
        }
      } catch (Exception e) {
        // Expected if mbean does not exist
      }

      ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);

    } catch (Exception e) {
      logger.error("Failed to register MBEAN", e);
    }
  }

  private void setAlertTrigger(
      Aggregation aggregation,
      AlertConfiguration newAlertConfig,
      Consumer<AlertBreach> alertObserver) {
    this.alertTrigger = new AlertPipelineTrigger(newAlertConfig, alertObserver);
    aggregation.setConsumer(alertTrigger);
  }

  @Override
  public OptionalDouble getValue() {
    return aggregation.compute();
  }

  @Override
  public void updateConfig(AlertConfiguration newAlertConfig) {
    this.alertConfiguration = newAlertConfig;
    setAlertTrigger(aggregation, newAlertConfig, alertObserver);
  }

  @Override
  public void track(TelemetryDataPoint telemetryDataPoint) {
    if (filter.test(telemetryDataPoint.getName())) {
      aggregation.update(telemetryDataPoint);
    }
  }

  @Override
  public long getCooldownSeconds() {
    return alertConfiguration.getCooldownSeconds();
  }

  @Override
  public long getProfilerDurationSeconds() {
    return alertConfiguration.getProfileDurationSeconds();
  }

  @Override
  public float getThreshold() {
    return alertConfiguration.getThreshold();
  }

  @Override
  public double getCurrentAverage() {
    return aggregation.compute().orElse(0.0d);
  }

  @Override
  public boolean getEnabled() {
    return alertConfiguration.isEnabled();
  }

  @Override
  public boolean isOffCooldown() {
    return this.alertTrigger.isOffCooldown();
  }

  @Override
  public String getLastAlertTime() {
    return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(this.alertTrigger.getLastAlertTime());
  }
}
