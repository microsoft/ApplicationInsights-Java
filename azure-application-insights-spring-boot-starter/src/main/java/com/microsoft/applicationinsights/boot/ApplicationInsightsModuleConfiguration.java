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

import com.microsoft.applicationinsights.boot.ApplicationInsightsProperties.HeartBeat;
import com.microsoft.applicationinsights.boot.HeartBeatProvider.SpringBootHeartBeatProvider;
import com.microsoft.applicationinsights.boot.initializer.SpringBootTelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.DeviceInfoContextInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.SdkVersionContextInitializer;
import com.microsoft.applicationinsights.internal.heartbeat.HeartBeatModule;
import com.microsoft.applicationinsights.internal.heartbeat.HeartbeatDefaultPayload;
import com.microsoft.applicationinsights.internal.perfcounter.JvmPerformanceCountersModule;
import com.microsoft.applicationinsights.internal.perfcounter.ProcessPerformanceCountersModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

/**
 * <h1>Core Application Insights Configuration</h1>
 * <p>
 *   This class provides the Core Configuration for ApplicationInsights. This configuration is
 *   irrespective of WebApplications. {@link Configuration} for non-web applications.
 * </p>
 *
 * @author Arthur Gavlyukovskiy, Dhaval Doshi
 */
@Configuration
@EnableConfigurationProperties(ApplicationInsightsProperties.class)
@ConditionalOnProperty(value = "azure.application-insights.enabled", havingValue = "true", matchIfMissing = true)
public class ApplicationInsightsModuleConfiguration {

    /**
     * Instance for the container of ApplicationInsights Properties
     */
    private ApplicationInsightsProperties applicationInsightsProperties;

    @Autowired
    public ApplicationInsightsModuleConfiguration(ApplicationInsightsProperties properties) {
        this.applicationInsightsProperties = properties;
    }

    /**
     * Bean for SdkVersionContextInitializer
     * @return instance of {@link SdkVersionContextInitializer}
     */
    @Bean
    public SdkVersionContextInitializer sdkVersionContextInitializer() {
        return new SdkVersionContextInitializer();
    }

    /**
     * Bean for DeviceInfoContextInitializer
     * @return instance of {@link DeviceInfoContextInitializer}
     */
    @Bean
    public DeviceInfoContextInitializer deviceInfoContextInitializer() {
        return new DeviceInfoContextInitializer();
    }

    /**
     * Bean for SpringBootTelemetryInitializer
     * @return instance of {@link SpringBootTelemetryInitializer}
     */
    @Bean
    public SpringBootTelemetryInitializer springBootTelemetryInitializer() {
        return new SpringBootTelemetryInitializer();
    }

    /**
     * Bean for ProcessPerformanceCounterModule
     * @return instance of {@link ProcessPerformanceCountersModule}
     */
    //FIXME: This should be conditional on operating System. However, current architecture of ProcessBuiltInPerformanceCountersFactory
    //FIXME: does not separate this concerns therefore cannot condition as of now.
    @Bean
    @DependsOn("performanceCounterContainer")
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.ProcessPerformanceCountersModule.enabled", havingValue = "true", matchIfMissing = true)
    public ProcessPerformanceCountersModule processPerformanceCountersModule() {
        try {
            return new ProcessPerformanceCountersModule();
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not initialize Windows performance counters module, " +
                    "please set property 'azure.application-insights.default-modules.ProcessPerformanceCountersModule.enabled=false' to avoid this error message.", e);
        }
    }

    /**
     * Bean for JvmPerformanceCounterModule
     * @return instance of {@link JvmPerformanceCountersModule}
     */
    @Bean
    @DependsOn("performanceCounterContainer")
    @ConditionalOnProperty(value = "azure.application-insights.default.modules.JvmPerformanceCountersModule.enabled", havingValue = "true", matchIfMissing = true)
    public JvmPerformanceCountersModule jvmPerformanceCountersModule() {
        try {
            return new JvmPerformanceCountersModule();
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not initialize Jvm Performance Counters module "
                + "please set the property 'azure.application-insights.default.modules.JvmPerformanceCountersModule.enabled=false' to "
                + "avoid this error message", e);
        }
    }

    /**
     * Bean for HeartBeatModule. This also sets the properties for HeartBeatModule
     * @param environment
     * @return initialized instance with user specified properties of {@link HeartBeatModule}
     */
    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default.modules.HeartBeat.enabled", havingValue = "true", matchIfMissing = true)
    public HeartBeatModule heartBeatModule(Environment environment) {
        try {
            HeartBeatModule heartBeatModule = new HeartBeatModule();
            HeartbeatDefaultPayload.addDefaultPayLoadProvider(new SpringBootHeartBeatProvider(environment));
            HeartBeat heartBeat = applicationInsightsProperties.getHeartBeat();
            heartBeatModule.setHeartBeatInterval(heartBeat.getHeartBeatInterval());
            if (heartBeat.getExcludedHeartBeatPropertiesList().size() > 0) {
                heartBeatModule.setExcludedHeartBeatProperties(heartBeat.getExcludedHeartBeatPropertiesList());
            }
            if (heartBeat.getExcludedHeartBeatProviderList().size() > 0) {
                heartBeatModule.setExcludedHeartBeatPropertiesProvider(heartBeat.getExcludedHeartBeatProviderList());
            }
            return heartBeatModule;
        }
        catch (Exception e) {
            throw new IllegalStateException("could not configure Heartbeat, please set 'azure.application-insights.default.modules.HearBeat.enabled'"
                + " to false ", e);
        }
    }
}
