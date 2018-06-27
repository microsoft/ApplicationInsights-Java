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

package com.microsoft.applicationinsights.boot;

import static org.slf4j.LoggerFactory.getLogger;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.boot.ApplicationInsightsProperties.Channel.InProcess;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounterContainer;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulse;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

/**
 *
 *
 * <h1>The central class for configuring and creating initialized {@link TelemetryConfiguration}
 * </h1>
 *
 * @author Arthur Gavlyukovskiy, Dhaval Doshi
 */
@Configuration
@ConditionalOnProperty(value = "azure.application-insights.instrumentation-key")
@EnableConfigurationProperties(ApplicationInsightsProperties.class)
@ConditionalOnClass(TelemetryConfiguration.class)
@Import({
  ApplicationInsightsModuleConfiguration.class,
  ApplicationInsightsWebModuleConfiguration.class
})
public class ApplicationInsightsTelemetryAutoConfiguration {

  private static final Logger log = getLogger(ApplicationInsightsTelemetryAutoConfiguration.class);

  private ApplicationInsightsProperties applicationInsightsProperties;

  private Collection<ContextInitializer> contextInitializers;

  private Collection<TelemetryInitializer> telemetryInitializers;

  private Collection<TelemetryModule> telemetryModules;

  private Collection<TelemetryProcessor> telemetryProcessors;

  @Autowired
  public ApplicationInsightsTelemetryAutoConfiguration(
      ApplicationInsightsProperties applicationInsightsProperties) {
    this.applicationInsightsProperties = applicationInsightsProperties;
  }

  @Autowired(required = false)
  public void setContextInitializers(Collection<ContextInitializer> contextInitializers) {
    this.contextInitializers = contextInitializers;
  }

  @Autowired(required = false)
  public void setTelemetryInitializers(Collection<TelemetryInitializer> telemetryInitializers) {
    this.telemetryInitializers = telemetryInitializers;
  }

  @Autowired(required = false)
  public void setTelemetryModules(Collection<TelemetryModule> telemetryModules) {
    this.telemetryModules = telemetryModules;
  }

  @Autowired(required = false)
  public void setTelemetryProcessors(Collection<TelemetryProcessor> telemetryProcessors) {
    this.telemetryProcessors = telemetryProcessors;
  }

  @Bean
  @DependsOn("internalLogger")
  public TelemetryConfiguration telemetryConfiguration(TelemetryChannel telemetryChannel) {
    TelemetryConfiguration telemetryConfiguration =
        TelemetryConfiguration.getActiveWithoutInitializingConfig();
    telemetryConfiguration.setTrackingIsDisabled(!applicationInsightsProperties.isEnabled());
    telemetryConfiguration.setInstrumentationKey(
        applicationInsightsProperties.getInstrumentationKey());
    if (contextInitializers != null) {
      telemetryConfiguration.getContextInitializers().addAll(contextInitializers);
    }
    if (telemetryInitializers != null) {
      telemetryConfiguration.getTelemetryInitializers().addAll(telemetryInitializers);
    }
    if (telemetryModules != null) {
      telemetryConfiguration.getTelemetryModules().addAll(telemetryModules);
    }
    if (telemetryProcessors != null) {
      telemetryConfiguration.getTelemetryProcessors().addAll(telemetryProcessors);
    }
    telemetryConfiguration.setChannel(telemetryChannel);
    initializeComponents(telemetryConfiguration);
    return telemetryConfiguration;
  }

  // TODO: copy-paste from TelemetryConfigurationFactory, move to TelemetryConfiguration?
  private void initializeComponents(TelemetryConfiguration configuration) {
    List<TelemetryModule> telemetryModules = configuration.getTelemetryModules();

    for (TelemetryModule module : telemetryModules) {
      try {
        module.initialize(configuration);
      } catch (Exception e) {
        log.error("Failed to initialized telemetry module " + module.getClass().getSimpleName(), e);
      }
    }
  }

  @Bean
  public TelemetryClient telemetryClient(TelemetryConfiguration configuration) {
    return new TelemetryClient(configuration);
  }

  @Bean
  @ConditionalOnMissingBean
  public TelemetryChannel telemetryChannel() {
    InProcess inProcess = applicationInsightsProperties.getChannel().getInProcess();
    return new InProcessTelemetryChannel(
        inProcess.getEndpointAddress(),
        String.valueOf(inProcess.getMaxTransmissionStorageFilesCapacityInMb()),
        inProcess.isDeveloperMode(),
        inProcess.getMaxTelemetryBufferCapacity(),
        inProcess.getFlushIntervalInSeconds(),
        inProcess.isThrottling(),
        inProcess.getMaxInstantRetry());
  }

  @Bean
  @ConditionalOnProperty(
    value = "azure.application-insights.quick-pulse.enabled",
    havingValue = "true",
    matchIfMissing = true
  )
  @DependsOn("telemetryConfiguration")
  public QuickPulse quickPulse() {
    QuickPulse.INSTANCE.initialize();
    return QuickPulse.INSTANCE;
  }

  @Bean
  public InternalLogger internalLogger() {
    Map<String, String> loggerParameters = new HashMap<>();
    ApplicationInsightsProperties.Logger logger = applicationInsightsProperties.getLogger();
    loggerParameters.put("Level", logger.getLevel().name());
    InternalLogger.INSTANCE.initialize(logger.getType().name(), loggerParameters);
    return InternalLogger.INSTANCE;
  }

  @Bean
  public PerformanceCounterContainer performanceCounterContainer() {
    ApplicationInsightsProperties.PerformanceCounter performanceCounter =
        applicationInsightsProperties.getPerformanceCounter();
    PerformanceCounterContainer.INSTANCE.setCollectionFrequencyInSec(
        performanceCounter.getCollectionFrequencyInSeconds());

    ApplicationInsightsProperties.Jmx jmx = applicationInsightsProperties.getJmx();
    if (jmx.getJmxCounters() != null && jmx.getJmxCounters().size() > 0) {
      applicationInsightsProperties.processAndLoadJmxCounters(jmx.getJmxCounters());
    }
    return PerformanceCounterContainer.INSTANCE;
  }
}
