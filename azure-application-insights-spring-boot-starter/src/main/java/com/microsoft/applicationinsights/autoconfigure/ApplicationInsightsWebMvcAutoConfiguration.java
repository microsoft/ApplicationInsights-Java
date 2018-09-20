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

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.web.internal.ApplicationInsightsServletContextListener;
import com.microsoft.applicationinsights.web.internal.WebRequestTrackingFilter;
import com.microsoft.applicationinsights.web.spring.internal.InterceptorRegistry;
import javax.servlet.ServletContextListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * <h1>Configuration for Auto-collection of HTTP requests.</h1>
 *
 * <p>
 *   This class is responsible for configuring {@link WebRequestTrackingFilter} for auto collection
 *   of incoming HTTP requests
 * </p>
 *
 * @author Arthur Gavlyukovskiy, Dhaval Doshi
 */

@Configuration
@Import(InterceptorRegistry.class)
@ConditionalOnBean(TelemetryConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(value = "azure.application-insights.web.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(ApplicationInsightsTelemetryAutoConfiguration.class)
public class ApplicationInsightsWebMvcAutoConfiguration {

    /**
     * Programmatically registers a FilterRegistrationBean to register WebRequestTrackingFilter
     * @param webRequestTrackingFilter
     * @return Bean of type {@link FilterRegistrationBean}
     */
    @Bean
    public FilterRegistrationBean webRequestTrackingFilterRegistrationBean(WebRequestTrackingFilter webRequestTrackingFilter) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(webRequestTrackingFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * Programmatically registers an ApplicationInsightsServletContextListener to destroy all the running threads.
     * @param applicationInsightsServletContextListener
     * @return Bean of type {@link ServletListenerRegistrationBean}
     */
    @Bean
    public ServletListenerRegistrationBean<ServletContextListener>
    appInsightsServletContextListenerRegistrationBean(ApplicationInsightsServletContextListener applicationInsightsServletContextListener) {
        ServletListenerRegistrationBean<ServletContextListener> srb =
            new ServletListenerRegistrationBean<>();
        srb.setListener(applicationInsightsServletContextListener);
        return srb;
    }

    /**
     * Creates bean of type WebRequestTrackingFilter for request tracking
     * @param applicationName Name of the application to bind filter to
     * @return {@link Bean} of type {@link WebRequestTrackingFilter}
     */
    @Bean
    @ConditionalOnMissingBean
    @DependsOn("telemetryConfiguration")
    public WebRequestTrackingFilter webRequestTrackingFilter(@Value("${spring.application.name:application}") String applicationName) {
        return new WebRequestTrackingFilter(applicationName);
    }

    /**
     * Creates Bean ApplicationInsightsServletContextListener for gracefull shutdown
     * @return {@link Bean} of type {@link ApplicationInsightsServletContextListener}
     */
    @Bean
    @ConditionalOnMissingBean
    public ApplicationInsightsServletContextListener applicationInsightsServletContextListener() {
        return new ApplicationInsightsServletContextListener();
    }
}

