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

package com.microsoft.applicationinsights.web.spring.internal;

import com.microsoft.applicationinsights.web.spring.RequestNameHandlerInterceptorAdapter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * This class registers the RequestNameHandlerInterceptorAdapter to the interceptors registry.
 * The registration enables the interceptor to extract the http request's controller and action names.
 *
 * This class extends {@link WebMvcConfigurationSupport} to add {@link RequestNameHandlerInterceptorAdapter}
 * instead of overriding @EnableWebMvc annotation. This is necessary to prevent breaking SpringBoot
 * auto-configuration.
 *
 * Recommendation from Spring framework : If WebMvcConfigurer does not expose some more advanced setting
 * that needs to be configured consider removing the @EnableWebMvc annotation and extending directly from
 * WebMvcConfigurationSupport or DelegatingWebMvcConfiguration.
 *
 * @see <a href="https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/config/annotation/EnableWebMvc.html">
 *   https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/config/annotation/EnableWebMvc.html</a>
 */
@Configuration
public class InterceptorRegistry extends WebMvcConfigurationSupport {

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new RequestNameHandlerInterceptorAdapter());
    }
}
