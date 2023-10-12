// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.profiler.ProfilingInitializer;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(AgentListener.class)
public class AfterAgentListener implements AgentListener {

  private static final Logger logger = LoggerFactory.getLogger(AfterAgentListener.class);

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    // only safe now to make HTTPS calls because Java SSL classes
    // trigger loading of java.util.logging (starting with Java 8u231)
    // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized.
    LazyHttpClient.safeToInitLatch.countDown();

    Configuration configuration = FirstEntryPoint.getConfiguration();
    PerformanceCounterInitializer.initialize(configuration);

    TelemetryClient telemetryClient = TelemetryClient.getActive();
    if (configuration.preview.profiler.enabled
        && telemetryClient != null
        && telemetryClient.getConnectionString() != null) {
      try {
        ProfilingInitializer.initialize(
            SecondEntryPoint.getTempDir(),
            configuration.preview.profiler,
            configuration.preview.gcEvents.reportingLevel,
            configuration.role.name,
            configuration.role.instance,
            TelemetryClient.getActive());
      } catch (RuntimeException e) {
        logger.warn("Failed to initialize profiler", e);
      }
    }
  }
}
