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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.boot.ApplicationInsightsTelemetryAutoConfiguration.EnabledAndHasInstrumentationKeyCondition;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.TelemetrySampler;
import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.internal.channel.sampling.FixedRateTelemetrySampler;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@EnableConfigurationProperties(ApplicationInsightsProperties.class)
@Conditional(EnabledAndHasInstrumentationKeyCondition.class)
@ConditionalOnClass(TelemetryConfiguration.class)
@Import({
        ApplicationInsightsModuleConfiguration.class,
        ApplicationInsightsWebModuleConfiguration.class
})
public class ApplicationInsightsTelemetryAutoConfiguration {

    private static final Logger log = getLogger(ApplicationInsightsTelemetryAutoConfiguration.class);

    @Autowired
    private ApplicationInsightsProperties applicationInsightsProperties;

    @Autowired(required = false)
    private Collection<ContextInitializer> contextInitializers;
    @Autowired(required = false)
    private Collection<TelemetryInitializer> telemetryInitializers;
    @Autowired(required = false)
    private Collection<TelemetryModule> telemetryModules;
    @Autowired(required = false)
    private Collection<TelemetryProcessor> telemetryProcessors;

    @Bean
    public TelemetryConfiguration telemetryConfiguration(TelemetryChannel telemetryChannel) {
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.getActive();
        telemetryConfiguration.setTrackingIsDisabled(!applicationInsightsProperties.isEnabled());
        telemetryConfiguration.setInstrumentationKey(applicationInsightsProperties.getInstrumentationKey());
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
            }
            catch (Exception e) {
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
    public TelemetrySampler telemetrySampler() {
        return new FixedRateTelemetrySampler();
    }

    @Bean
    @ConditionalOnMissingBean
    public TelemetryChannel telemetryChannel(TelemetrySampler telemetrySampler) {
        ApplicationInsightsProperties.Channel.InProcess inProcess = applicationInsightsProperties.getChannel().getInProcess();
        InProcessTelemetryChannel telemetryChannel = new InProcessTelemetryChannel(inProcess.getEndpointAddress(),
                /*String.valueOf(inProcess.getMaxTransmissionStorageFilesCapacityInMb()),*/ inProcess.isDeveloperMode(),
                inProcess.getMaxTelemetryBufferCapacity(), inProcess.getFlushIntervalInSeconds()/*, inProcess.isThrottling()*/);
        telemetryChannel.setSampler(telemetrySampler);
        return telemetryChannel;
    }

    @Bean
    public InternalLogger internalLogger() {
        Map<String, String> loggerParameters = new HashMap<>();
        ApplicationInsightsProperties.Logger logger = applicationInsightsProperties.getLogger();
        loggerParameters.put("Level", logger.getLevel().name());
        InternalLogger.INSTANCE.initialize(logger.getType().name(), loggerParameters);
        return InternalLogger.INSTANCE;
    }

    static class EnabledAndHasInstrumentationKeyCondition extends AllNestedConditions {

        EnabledAndHasInstrumentationKeyCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "azure.application-insights.enabled", matchIfMissing = true)
        static class OnEnabled {
        }


        @ConditionalOnProperty(value = "azure.application-insights.instrumentation-key")
        static class OnInstrumentationKeySet {
        }
    }
}
