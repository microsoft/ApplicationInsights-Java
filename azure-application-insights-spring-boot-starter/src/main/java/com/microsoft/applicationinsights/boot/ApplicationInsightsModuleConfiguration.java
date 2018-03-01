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

import com.microsoft.applicationinsights.boot.conditional.ConditionalOnOperatingSystem;
import com.microsoft.applicationinsights.boot.conditional.OperatingSystem;
import com.microsoft.applicationinsights.boot.initializer.SpringBootContextInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.DeviceInfoContextInitializer;
import com.microsoft.applicationinsights.extensibility.initializer.SdkVersionContextInitializer;
import com.microsoft.applicationinsights.internal.perfcounter.ProcessPerformanceCountersModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link Configuration} for non-web applications.
 *
 * @author Arthur Gavlyukovskiy
 */
@Configuration
public class ApplicationInsightsModuleConfiguration {

    @Bean
    public SpringBootContextInitializer springBootContextInitializer(Environment environment) {
        return new SpringBootContextInitializer(environment);
    }

    @Bean
    public SdkVersionContextInitializer sdkVersionContextInitializer() {
        return new SdkVersionContextInitializer();
    }

    @Bean
    public DeviceInfoContextInitializer deviceInfoContextInitializer() {
        return new DeviceInfoContextInitializer();
    }

    @Bean
    @ConditionalOnOperatingSystem(OperatingSystem.WINDOWS)
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
}
