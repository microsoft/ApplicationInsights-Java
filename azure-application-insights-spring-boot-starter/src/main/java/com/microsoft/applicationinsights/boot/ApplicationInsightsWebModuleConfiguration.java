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

import com.microsoft.applicationinsights.web.extensibility.initializers.WebOperationIdTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebOperationNameTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebSessionTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebUserAgentTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebUserTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.modules.WebRequestTrackingTelemetryModule;
import com.microsoft.applicationinsights.web.extensibility.modules.WebSessionTrackingTelemetryModule;
import com.microsoft.applicationinsights.web.extensibility.modules.WebUserTrackingTelemetryModule;
import com.microsoft.applicationinsights.web.internal.perfcounter.WebPerformanceCounterModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * {@link Configuration} for web applications.
 *
 * @author Arthur Gavlyukovskiy
 */
@Configuration
@ConditionalOnProperty(value = "azure.application-insights.web.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication
public class ApplicationInsightsWebModuleConfiguration {

    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebRequestTrackingTelemetryModule.enabled", havingValue = "true", matchIfMissing = true)
    public WebRequestTrackingTelemetryModule webRequestTrackingTelemetryModule() {
        return new WebRequestTrackingTelemetryModule();
    }

    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebSessionTrackingTelemetryModule.enabled", havingValue = "true", matchIfMissing = true)
    public WebSessionTrackingTelemetryModule webSessionTrackingTelemetryModule() {
        return new WebSessionTrackingTelemetryModule();
    }

    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebUserTrackingTelemetryModule.enabled", havingValue = "true", matchIfMissing = true)
    public WebUserTrackingTelemetryModule webUserTrackingTelemetryModule() {
        return new WebUserTrackingTelemetryModule();
    }

    @Bean
    @DependsOn("performanceCounterContainer")
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebPerformanceCounterModule.enabled", havingValue = "true", matchIfMissing = true)
    public WebPerformanceCounterModule webPerformanceCounterModule() {
        return new WebPerformanceCounterModule();
    }

    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebOperationIdTelemetryInitializer.enabled", havingValue = "true", matchIfMissing = true)
    public WebOperationIdTelemetryInitializer webOperationIdTelemetryInitializer() {
        return new WebOperationIdTelemetryInitializer();
    }

    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebOperationNameTelemetryInitializer.enabled", havingValue = "true", matchIfMissing = true)
    public WebOperationNameTelemetryInitializer webOperationNameTelemetryInitializer() {
        return new WebOperationNameTelemetryInitializer();
    }

    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebSessionTelemetryInitializer.enabled", havingValue = "true", matchIfMissing = true)
    public WebSessionTelemetryInitializer webSessionTelemetryInitializer() {
        return new WebSessionTelemetryInitializer();
    }

    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebUserTelemetryInitializer.enabled", havingValue = "true", matchIfMissing = true)
    public WebUserTelemetryInitializer webUserTelemetryInitializer() {
        return new WebUserTelemetryInitializer();
    }

    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebUserAgentTelemetryInitializer.enabled", havingValue = "true", matchIfMissing = true)
    public WebUserAgentTelemetryInitializer webUserAgentTelemetryInitializer() {
        return new WebUserAgentTelemetryInitializer();
    }
}
