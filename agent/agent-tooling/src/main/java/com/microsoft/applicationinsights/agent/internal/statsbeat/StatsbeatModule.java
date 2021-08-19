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

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import com.microsoft.applicationinsights.agent.internal.common.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsbeatModule {

  private static final Logger logger = LoggerFactory.getLogger(BaseStatsbeat.class);

  private static final StatsbeatModule instance = new StatsbeatModule();

  private static final ScheduledExecutorService scheduledExecutor =
      Executors.newSingleThreadScheduledExecutor(
          ThreadPoolUtils.createDaemonThreadFactory(BaseStatsbeat.class));

  private final CustomDimensions customDimensions;

  private final NetworkStatsbeat networkStatsbeat;
  private final AttachStatsbeat attachStatsbeat;
  private final FeatureStatsbeat featureStatsbeat;

  private final AtomicBoolean started = new AtomicBoolean();

  private volatile TelemetryClient telemetryClient;

  private StatsbeatModule() {
    customDimensions = new CustomDimensions();
    networkStatsbeat = new NetworkStatsbeat(customDimensions);
    attachStatsbeat = new AttachStatsbeat(customDimensions);
    featureStatsbeat = new FeatureStatsbeat(customDimensions);
  }

  public void start(TelemetryClient telemetryClient, Configuration config) {
    if (started.getAndSet(true)) {
      throw new IllegalStateException("initialize already called");
    }

    if (this.telemetryClient == null) {
      this.telemetryClient = telemetryClient;
      networkStatsbeat.setCurrentHost(networkStatsbeat.getHost(telemetryClient.getEndpointProvider().getIngestionEndpointUrl().toString()));
    }

    long intervalSeconds = config.internal.statsbeat.intervalSeconds;
    long featureIntervalSeconds = config.internal.statsbeat.featureIntervalSeconds;

    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(networkStatsbeat, telemetryClient),
        intervalSeconds,
        intervalSeconds,
        TimeUnit.SECONDS);
    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(attachStatsbeat, telemetryClient),
        intervalSeconds,
        intervalSeconds,
        TimeUnit.SECONDS);
    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(featureStatsbeat, telemetryClient),
        featureIntervalSeconds,
        featureIntervalSeconds,
        TimeUnit.SECONDS);

    ResourceProvider rp = customDimensions.getResourceProvider();
    // only turn on AzureMetadataService when the resource provider is VM or UNKNOWN.
    // and it's not necessary to make this call.
    if (rp == ResourceProvider.RP_VM || rp == ResourceProvider.UNKNOWN) {
      // will only reach here the first time, after instance has been instantiated
      new AzureMetadataService(attachStatsbeat, customDimensions)
          .scheduleWithFixedDelay(intervalSeconds);
    }

    featureStatsbeat.trackConfigurationOptions(config);
  }

  public static StatsbeatModule get() {
    return instance;
  }

  public NetworkStatsbeat getNetworkStatsbeat() {
    return networkStatsbeat;
  }

  // send network statsbeat whenever redirect happens since url has been changed.
  // new url is always retrieved from the redirect policy cache map and we don't update the endpoint.
  public void sendNetworkStatsbeatOnRedirect(String redirectUrl) {
    networkStatsbeat.trackHostOnRedirect(telemetryClient, redirectUrl);
    StatsbeatSender sender = new StatsbeatSender(networkStatsbeat, telemetryClient);
    Thread senderThread = new Thread(sender);
    senderThread.setDaemon(true);
    senderThread.start();
  }

  /** Runnable which is responsible for calling the send method to transmit Statsbeat telemetry. */
  private static class StatsbeatSender implements Runnable {

    private final BaseStatsbeat statsbeat;
    private final TelemetryClient telemetryClient;

    private StatsbeatSender(BaseStatsbeat statsbeat, TelemetryClient telemetryClient) {
      this.statsbeat = statsbeat;
      this.telemetryClient = telemetryClient;
    }

    @Override
    public void run() {
      try {
        // For Linux Consumption Plan, connection string is lazily set.
        // There is no need to send statsbeat when cikey is empty.
        String customerIkey = telemetryClient.getInstrumentationKey();
        if (customerIkey == null || customerIkey.isEmpty()) {
          return;
        }
        statsbeat.send(telemetryClient);
      } catch (RuntimeException e) {
        logger.error("Error occurred while sending statsbeat", e);
      }
    }
  }
}
