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

package com.azure.monitor.opentelemetry.exporter.implementation.heartbeat;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryItemExporter;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concrete implementation of Heartbeat functionality. */
public class HeartbeatExporter {

  private static final Logger logger = LoggerFactory.getLogger(HeartbeatExporter.class);

  /** The name of the heartbeat metric. */
  private static final String HEARTBEAT_SYNTHETIC_METRIC_NAME = "HeartbeatState";

  /** The counter for heartbeat sent to portal. */
  private long heartbeatsSent;

  /** Map to hold heartbeat properties. */
  private final ConcurrentMap<String, HeartBeatPropertyPayload> heartbeatProperties;

  /** Telemetry item exporter used to send heartbeat. */
  private final TelemetryItemExporter telemetryItemExporter;

  /** Telemetry builder consumer used to populate defaults properties. */
  private final Consumer<AbstractTelemetryBuilder> telemetryInitializer;

  /** ThreadPool used for adding properties to concurrent dictionary. */
  private final ExecutorService propertyUpdateService;

  /** Threadpool used to send data heartbeat telemetry. */
  private final ScheduledExecutorService heartBeatSenderService;

  public static void start(
      long intervalSeconds,
      Consumer<AbstractTelemetryBuilder> telemetryInitializer,
      TelemetryItemExporter telemetryItemExporter) {
    new HeartbeatExporter(intervalSeconds, telemetryInitializer, telemetryItemExporter);
  }

  public HeartbeatExporter(
      long intervalSeconds,
      Consumer<AbstractTelemetryBuilder> telemetryInitializer,
      TelemetryItemExporter telemetryItemExporter) {
    this.heartbeatProperties = new ConcurrentHashMap<>();
    this.heartbeatsSent = 0;
    this.propertyUpdateService =
        Executors.newCachedThreadPool(
            ThreadPoolUtils.createDaemonThreadFactory(
                HeartbeatExporter.class, "propertyUpdateService"));
    this.heartBeatSenderService =
        Executors.newSingleThreadScheduledExecutor(
            ThreadPoolUtils.createDaemonThreadFactory(
                HeartbeatExporter.class, "heartBeatSenderService"));

    this.telemetryItemExporter = telemetryItemExporter;
    this.telemetryInitializer = telemetryInitializer;

    // Submit task to set properties to dictionary using separate thread. we do not wait for the
    // results to come out as some I/O bound properties may take time.
    propertyUpdateService.submit(HeartbeatDefaultPayload.populateDefaultPayload(this));

    heartBeatSenderService.scheduleAtFixedRate(
        this::send, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
  }

  public boolean addHeartBeatProperty(
      String propertyName, String propertyValue, boolean isHealthy) {

    if (heartbeatProperties.containsKey(propertyName)) {
      logger.trace(
          "heartbeat property {} cannot be added twice. Please use setHeartBeatProperty instead to modify the value",
          propertyName);
      return false;
    }

    HeartBeatPropertyPayload payload = new HeartBeatPropertyPayload();
    payload.setHealthy(isHealthy);
    payload.setPayloadValue(propertyValue);
    heartbeatProperties.put(propertyName, payload);
    logger.trace("added heartbeat property {} - {}", propertyName, propertyValue);
    return true;
  }

  /** Send the heartbeat item synchronously to application insights backend. */
  private void send() {
    try {
      telemetryItemExporter.send(Collections.singletonList(gatherData()));
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
    MetricTelemetryBuilder telemetryBuilder =
        MetricTelemetryBuilder.create(HEARTBEAT_SYNTHETIC_METRIC_NAME, numHealthy);
    telemetryInitializer.accept(telemetryBuilder);
    telemetryBuilder.addTag(
        ContextTagKeys.AI_OPERATION_SYNTHETIC_SOURCE.toString(), HEARTBEAT_SYNTHETIC_METRIC_NAME);

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }

    return telemetryBuilder.build();
  }
}
