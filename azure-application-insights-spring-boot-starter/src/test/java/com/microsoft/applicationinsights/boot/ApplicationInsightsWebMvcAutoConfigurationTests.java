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

import com.microsoft.applicationinsights.internal.quickpulse.QuickPulse;
import com.microsoft.applicationinsights.web.internal.WebRequestTrackingFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arthur Gavlyukovskiy
 */
@SpringBootTest(
        properties = {
                "spring.test.mockmvc: true",
                "spring.application.name: test-application",
                "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000"
        },
        classes = {
                EmbeddedServletContainerAutoConfiguration.class,
                ServerPropertiesAutoConfiguration.class,
                DispatcherServletAutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class,
                WebMvcAutoConfiguration.class,
                MockMvcAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class,
                ApplicationInsightsWebMvcAutoConfiguration.class
        },
        webEnvironment = WebEnvironment.RANDOM_PORT
)
@RunWith(SpringRunner.class)
public class ApplicationInsightsWebMvcAutoConfigurationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    public void shouldRegisterWebRequestTrackingFilter() {
        WebRequestTrackingFilter webRequestTrackingFilter = context.getBean(WebRequestTrackingFilter.class);

        assertThat(webRequestTrackingFilter).extracting("appName").contains("test-application");
        assertThat(QuickPulse.INSTANCE).extracting("initialized").contains(true);
    }

}