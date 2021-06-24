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

package com.microsoft.applicationinsights.internal.heartbeat;

import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.internal.FormattedTime;
import com.microsoft.applicationinsights.internal.TelemetryClient;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete implementation of Heartbeat functionality. This class implements {@link
 * com.microsoft.applicationinsights.internal.heartbeat.HeartBeatProviderInterface}
 */
public class HeartBeatProvider implements HeartBeatProviderInterface {

  private static final Logger logger = LoggerFactory.getLogger(HeartBeatProvider.class);

  /** The name of the heartbeat metric. */
  private static final String HEARTBEAT_SYNTHETIC_METRIC_NAME = "HeartbeatState";

  /** The list of disabled properties. */
  private List<String> disableDefaultProperties = new ArrayList<>();

  /** List of disabled heartbeat providers. */
  private List<String> disabledHeartBeatPropertiesProviders = new ArrayList<>();

  /** The counter for heartbeat sent to portal. */
  private long heartbeatsSent;

  /** Map to hold heartbeat properties. */
  private final ConcurrentMap<String, HeartBeatPropertyPayload> heartbeatProperties;

  /** Interval at which heartbeat would be sent. */
  private long interval;

  /** Telemetry client instance used to send heartbeat. */
  private TelemetryClient telemetryClient;

  /** ThreadPool used for adding properties to concurrent dictionary. */
  private final ExecutorService propertyUpdateService;

  /** Threadpool used to send data heartbeat telemetry. */
  private final ScheduledExecutorService heartBeatSenderService;

  /** Heartbeat enabled state. */
  private volatile boolean isEnabled;

  public HeartBeatProvider() {
    this.interval = HeartBeatProviderInterface.DEFAULT_HEARTBEAT_INTERVAL;
    this.heartbeatProperties = new ConcurrentHashMap<>();
    this.isEnabled = true;
    this.heartbeatsSent = 0;
    this.propertyUpdateService =
        Executors.newCachedThreadPool(
            ThreadPoolUtils.createDaemonThreadFactory(
                HeartBeatProvider.class, "propertyUpdateService"));
    this.heartBeatSenderService =
        Executors.newSingleThreadScheduledExecutor(
            ThreadPoolUtils.createDaemonThreadFactory(
                HeartBeatProvider.class, "heartBeatSenderService"));
  }

  @Override
  public void initialize(TelemetryClient telemetryClient) {
    if (isEnabled) {
      if (this.telemetryClient == null) {
        this.telemetryClient = telemetryClient;
      }

      // Submit task to set properties to dictionary using separate thread. we do not wait for the
      // results to come out as some I/O bound properties may take time.
      propertyUpdateService.submit(
          HeartbeatDefaultPayload.populateDefaultPayload(
              getExcludedHeartBeatProperties(), getExcludedHeartBeatPropertyProviders(), this));

      heartBeatSenderService.scheduleAtFixedRate(this::send, interval, interval, TimeUnit.SECONDS);
    }
  }

  @Override
  public boolean addHeartBeatProperty(
      String propertyName, String propertyValue, boolean isHealthy) {

    boolean isAdded = false;
    if (!StringUtils.isEmpty(propertyName)) {
      if (!heartbeatProperties.containsKey(propertyName)) {
        HeartBeatPropertyPayload payload = new HeartBeatPropertyPayload();
        payload.setHealthy(isHealthy);
        payload.setPayloadValue(propertyValue);
        heartbeatProperties.put(propertyName, payload);
        isAdded = true;
        logger.trace("added heartbeat property {} - {}", propertyName, propertyValue);
      } else {
        logger.trace(
            "heartbeat property {} cannot be added twice. Please use setHeartBeatProperty instead to modify the value",
            propertyName);
      }
    } else {
      logger.warn("cannot add property without property name");
    }
    return isAdded;
  }

  @Override
  public boolean isHeartBeatEnabled() {
    return isEnabled;
  }

  @Override
  public void setHeartBeatEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  @Override
  public List<String> getExcludedHeartBeatPropertyProviders() {
    return this.disabledHeartBeatPropertiesProviders;
  }

  @Override
  public void setExcludedHeartBeatPropertyProviders(
      List<String> excludedHeartBeatPropertyProviders) {
    this.disabledHeartBeatPropertiesProviders = excludedHeartBeatPropertyProviders;
  }

  @Override
  public long getHeartBeatInterval() {
    return this.interval;
  }

  @Override
  public void setHeartBeatInterval(long timeUnit) {
    // user set time unit in seconds
    this.interval = Math.max(timeUnit, HeartBeatProviderInterface.MINIMUM_HEARTBEAT_INTERVAL);
  }

  @Override
  public List<String> getExcludedHeartBeatProperties() {
    return this.disableDefaultProperties;
  }

  @Override
  public void setExcludedHeartBeatProperties(List<String> excludedHeartBeatProperties) {
    this.disableDefaultProperties = excludedHeartBeatProperties;
  }

  /** Send the heartbeat item synchronously to application insights backend. */
  private void send() {
    try {
      TelemetryItem telemetry = gatherData();
      telemetry
          .getTags()
          .put(
              ContextTagKeys.AI_OPERATION_SYNTHETIC_SOURCE.toString(),
              HEARTBEAT_SYNTHETIC_METRIC_NAME);
      telemetryClient.trackAsync(telemetry);
      logger.trace("No of heartbeats sent, {}", ++heartbeatsSent);
    } catch (RuntimeException e) {
      logger.warn("Error occured while sending heartbeat");
    }
  }

  /**
   * Creates and returns the heartbeat telemetry.
   *
   * @return Metric Telemetry which represent heartbeat.
   */
  // visible for testing
  TelemetryItem gatherData() {
    Map<String, String> properties = new HashMap<>();
    double numHealthy = 0;
    for (Map.Entry<String, HeartBeatPropertyPayload> entry : heartbeatProperties.entrySet()) {
      HeartBeatPropertyPayload payload = entry.getValue();
      properties.put(entry.getKey(), payload.getPayloadValue());
      numHealthy += payload.isHealthy() ? 0 : 1;
    }
    TelemetryItem telemetry = new TelemetryItem();
    MetricsData data = new MetricsData();
    MetricDataPoint point = new MetricDataPoint();
    telemetryClient.initMetricTelemetry(telemetry, data, point);

    point.setName(HEARTBEAT_SYNTHETIC_METRIC_NAME);
    point.setValue(numHealthy);
    point.setDataPointType(DataPointType.MEASUREMENT);

    data.setProperties(properties);

    telemetry.setTime(FormattedTime.fromNow());

    return telemetry;
  }
}
