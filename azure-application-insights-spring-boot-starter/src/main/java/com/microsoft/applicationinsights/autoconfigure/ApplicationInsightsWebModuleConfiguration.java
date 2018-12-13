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

package com.microsoft.applicationinsights.autoconfigure;

import com.microsoft.applicationinsights.web.extensibility.initializers.WebOperationIdTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebOperationNameTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebSessionTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebUserAgentTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.initializers.WebUserTelemetryInitializer;
import com.microsoft.applicationinsights.web.extensibility.modules.WebRequestTrackingTelemetryModule;
import com.microsoft.applicationinsights.web.extensibility.modules.WebSessionTrackingTelemetryModule;
import com.microsoft.applicationinsights.web.extensibility.modules.WebUserTrackingTelemetryModule;
import com.microsoft.applicationinsights.web.internal.perfcounter.WebPerformanceCounterModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h1>Web Configuration for Application Insights</h1>
 *
 * <p>
 *   This class provides the configuration for applications of type web. The modules in this class
 *   will only be configured if the Spring Framework identifies them as web Application.
 * </p>
 *
 * {@link Configuration} for web applications.
 *
 * @author Arthur Gavlyukovskiy
 */
@Configuration
@ConditionalOnProperty(value = "azure.application-insights.web.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication
class ApplicationInsightsWebModuleConfiguration {


    /**
     * Instance for the container of ApplicationInsights Properties
     */
    private ApplicationInsightsProperties applicationInsightsProperties;

    @Autowired
    public ApplicationInsightsWebModuleConfiguration(ApplicationInsightsProperties properties) {
        this.applicationInsightsProperties = properties;
    }

    /**
   * Bean for WebRequestTrackingTelemetryModule
   * @return instance of {@link WebRequestTrackingTelemetryModule}
   */
    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebRequestTrackingTelemetryModule.enabled", havingValue = "true", matchIfMissing = true)
    WebRequestTrackingTelemetryModule webRequestTrackingTelemetryModule() {
        WebRequestTrackingTelemetryModule w = new WebRequestTrackingTelemetryModule();
        w.isW3CEnabled = applicationInsightsProperties.getWeb().isEnableW3C();
        return w;
    }

  /**
   * Bean for WebSessionTrackingTelemetryModule
   * @return instance of {@link WebSessionTrackingTelemetryModule}
   */
  @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebSessionTrackingTelemetryModule.enabled", havingValue = "true", matchIfMissing = true)
    WebSessionTrackingTelemetryModule webSessionTrackingTelemetryModule() {
        return new WebSessionTrackingTelemetryModule();
    }

  /**
   * Bean for WebUserTrackingTelemetryModule
   * @return instance of {@link WebUserTrackingTelemetryModule}
   */
  @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebUserTrackingTelemetryModule.enabled", havingValue = "true", matchIfMissing = true)
    WebUserTrackingTelemetryModule webUserTrackingTelemetryModule() {
        return new WebUserTrackingTelemetryModule();
    }

  /**
   * Bean for WebPerformanceCounterModule
   * @return instance of {@link WebPerformanceCounterModule}
   */
  @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebPerformanceCounterModule.enabled", havingValue = "true", matchIfMissing = true)
    WebPerformanceCounterModule webPerformanceCounterModule() {
        return new WebPerformanceCounterModule();
    }

  /**
   * Bean for WebOperationIdTelemetryInitializer
   * @return instance of {@link WebOperationIdTelemetryInitializer}
   */
  @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebOperationIdTelemetryInitializer.enabled", havingValue = "true", matchIfMissing = true)
    WebOperationIdTelemetryInitializer webOperationIdTelemetryInitializer() {
        return new WebOperationIdTelemetryInitializer();
    }

    @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebOperationNameTelemetryInitializer.enabled", havingValue = "true", matchIfMissing = true)
    WebOperationNameTelemetryInitializer webOperationNameTelemetryInitializer() {
        return new WebOperationNameTelemetryInitializer();
    }

  /**
   * Bean for WebSessionTelemetryInitializer
   * @return instance of {@link WebSessionTelemetryInitializer}
   */
  @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebSessionTelemetryInitializer.enabled", havingValue = "true", matchIfMissing = true)
    WebSessionTelemetryInitializer webSessionTelemetryInitializer() {
        return new WebSessionTelemetryInitializer();
    }

  /**
   * Bean for WebUserTelemetryInitializer
   * @return instance of {@link WebUserTelemetryInitializer}
   */
  @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebUserTelemetryInitializer.enabled", havingValue = "true", matchIfMissing = true)
    WebUserTelemetryInitializer webUserTelemetryInitializer() {
        return new WebUserTelemetryInitializer();
    }

  /**
   * Bean for WebUserAgentTelemetryInitializer
   * @return instance of {@link WebUserAgentTelemetryInitializer}
   */
  @Bean
    @ConditionalOnProperty(value = "azure.application-insights.default-modules.WebUserAgentTelemetryInitializer.enabled", havingValue = "true", matchIfMissing = true)
    WebUserAgentTelemetryInitializer webUserAgentTelemetryInitializer() {
        return new WebUserAgentTelemetryInitializer();
    }
}
