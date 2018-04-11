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

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.internal.channel.samplingV2.FixedRateSamplingTelemetryProcessor;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.perfcounter.JvmPerformanceCountersModule;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounter;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounterContainer;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.web.extensibility.modules.WebUserTrackingTelemetryModule;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author Arthur Gavlyukovskiy, Dhaval Doshi
 */
public final class ApplicationInsightsTelemetryAutoConfigurationTests {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @After
    public void restore() {
        context.close();
    }

    @Test
    public void shouldSetInstrumentationKeyWhenContextLoads() {
        EnvironmentTestUtils.addEnvironment(context,
                "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000");
        context.register(PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        TelemetryClient telemetryClient = context.getBean(TelemetryClient.class);
        TelemetryConfiguration telemetryConfiguration = context.getBean(TelemetryConfiguration.class);

        assertThat(telemetryConfiguration).isSameAs(TelemetryConfiguration.getActive());
        assertThat(telemetryConfiguration.getInstrumentationKey()).isEqualTo("00000000-0000-0000-0000-000000000000");
        assertThat(telemetryClient.getContext().getInstrumentationKey()).isEqualTo("00000000-0000-0000-0000-000000000000");
    }

    @Test
    public void shouldSetInstrumentationKeyFromRelaxedCase() {
        EnvironmentTestUtils.addEnvironment(context,
                "AZURE.APPLICATION_INSIGHTS.INSTRUMENTATION_KEY: 00000000-0000-0000-0000-000000000000");
        context.register(PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        TelemetryClient telemetryClient = context.getBean(TelemetryClient.class);
        TelemetryConfiguration telemetryConfiguration = context.getBean(TelemetryConfiguration.class);

        assertThat(telemetryConfiguration).isSameAs(TelemetryConfiguration.getActive());
        assertThat(telemetryConfiguration.getInstrumentationKey()).isEqualTo("00000000-0000-0000-0000-000000000000");
        assertThat(telemetryClient.getContext().getInstrumentationKey()).isEqualTo("00000000-0000-0000-0000-000000000000");
    }

    @Test
    @Ignore
    public void shouldReloadInstrumentationKeyOnTelemetryClient() {
        TelemetryClient myClient = new TelemetryClient();

        EventTelemetry eventTelemetry1 = new EventTelemetry("test1");
        myClient.trackEvent(eventTelemetry1);
        assertThat(eventTelemetry1.getTimestamp()).isNull();

        EnvironmentTestUtils.addEnvironment(context,
                "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000");
        context.register(PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        EventTelemetry eventTelemetry2 = new EventTelemetry("test2");
        myClient.trackEvent(eventTelemetry2);
        assertThat(eventTelemetry2.getTimestamp()).describedAs("Expecting telemetry event to be sent").isNotNull();
    }

    @Test
    public void shouldNotFailIfInstrumentationKeyIsNotSet() {
        context.register(PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        assertThat(context.getBeansOfType(TelemetryClient.class)).isEmpty();
        assertThat(context.getBeansOfType(TelemetryConfiguration.class)).isEmpty();
    }

    @Test
    public void shouldBeAbleToDisableInstrumentationByProperty() {
        EnvironmentTestUtils.addEnvironment(context,
                "azure.application-insights.enabled: false",
                "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000");
        context.register(PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        TelemetryConfiguration telemetryConfiguration = context.getBean(TelemetryConfiguration.class);
        assertThat(telemetryConfiguration.isTrackingDisabled()).isTrue();
    }

    @Test
    public void shouldBeAbleToConfigureTelemetryChannel() {
        EnvironmentTestUtils.addEnvironment(context,
                "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000",
                "azure.application-insights.channel.in-process.developer-mode=false",
                "azure.application-insights.channel.in-process.flush-interval-in-seconds=123",
                "azure.application-insights.channel.in-process.max-telemetry-buffer-capacity=10");
        context.register(PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        TelemetryConfiguration telemetryConfiguration = context.getBean(TelemetryConfiguration.class);
        TelemetryChannel channel = telemetryConfiguration.getChannel();

        assertThat(channel).isInstanceOf(InProcessTelemetryChannel.class);
        assertThat(channel.isDeveloperMode()).isFalse();
        assertThat(channel).extracting("telemetryBuffer").extracting("transmitBufferTimeoutInSeconds").contains(123);
        assertThat(channel).extracting("telemetryBuffer").extracting("maxTelemetriesInBatch").contains(10);
    }

    @Test
    public void shouldBeAbleToConfigureSamplingTelemetryProcessor() {
        EnvironmentTestUtils.addEnvironment(context,
                "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000",
                "azure.application-insights.telemetry-processor.sampling.percentage=50",
                "azure.application-insights.telemetry-processor.sampling.include=Request");
        context.register(PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        TelemetryConfiguration telemetryConfiguration = context.getBean(TelemetryConfiguration.class);
        FixedRateSamplingTelemetryProcessor fixedRateSamplingTelemetryProcessor = context.getBean(FixedRateSamplingTelemetryProcessor.class);

        assertThat(telemetryConfiguration.getTelemetryProcessors()).extracting("class").contains(FixedRateSamplingTelemetryProcessor.class);
        assertThat(fixedRateSamplingTelemetryProcessor).extracting("samplingPercentage").contains(50.);
        assertThat(fixedRateSamplingTelemetryProcessor.getIncludedTypes()).contains(RequestTelemetry.class);
        assertThat(fixedRateSamplingTelemetryProcessor.getExcludedTypes()).isEmpty();
    }

    @Test
    public void shouldBeAbleToDisableAllWebModules() {
        EnvironmentTestUtils.addEnvironment(context,
                "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000",
                "azure.application-insights.web.enabled=false");
        context.register(PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        assertThat(context.getBeansOfType(WebUserTrackingTelemetryModule.class)).isEmpty();
    }

    @Test
    public void internalLoggerShouldBeInitializedBeforeTelemetryConfiguration() {
        EnvironmentTestUtils.addEnvironment(context,
            "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000",
            "azure.application-insights.logger.level=INFO"
            );
        context.register(PropertyPlaceholderAutoConfiguration.class,
            ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();
        InternalLogger logger = context.getBean(InternalLogger.class);
        assertThat(logger.isInfoEnabled()).isEqualTo(true);
    }

    @Test
    public void shouldBeAbleToDisableDefaultModules() {
        EnvironmentTestUtils.addEnvironment(context,
                "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000",
                "azure.application-insights.default-modules.WebUserTrackingTelemetryModule.enabled=false");
        context.register(PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        assertThat(context.getBeansOfType(WebUserTrackingTelemetryModule.class)).isEmpty();
    }

    @Test
    public void shouldBeAbleToAddCustomModules() {
        EnvironmentTestUtils.addEnvironment(context,
                "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000");
        context.register(PropertyPlaceholderAutoConfiguration.class,
                ApplicationInsightsTelemetryAutoConfiguration.class,
                CustomModuleConfiguration.class);
        context.refresh();

        TelemetryConfiguration telemetryConfiguration = context.getBean(TelemetryConfiguration.class);

        ContextInitializer myContextInitializer = context.getBean("myContextInitializer", ContextInitializer.class);
        TelemetryInitializer myTelemetryInitializer = context.getBean("myTelemetryInitializer", TelemetryInitializer.class);
        TelemetryModule myTelemetryModule = context.getBean("myTelemetryModule", TelemetryModule.class);
        TelemetryProcessor myTelemetryProcessor = context.getBean("myTelemetryProcessor", TelemetryProcessor.class);

        assertThat(telemetryConfiguration.getContextInitializers()).contains(myContextInitializer);
        assertThat(telemetryConfiguration.getTelemetryInitializers()).contains(myTelemetryInitializer);
        assertThat(telemetryConfiguration.getTelemetryModules()).contains(myTelemetryModule);
        assertThat(telemetryConfiguration.getTelemetryProcessors()).contains(myTelemetryProcessor);
    }

    @Test
    public void shouldBeAbleToConfigureJmxPerformanceCounters() throws Exception{
        EnvironmentTestUtils.addEnvironment(context,
            "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000",
            "azure.application-insights.jmx.jmx-counters:"
                + "java.lang:type=ClassLoading/LoadedClassCount/Current Loaded Class Count");
        context.register(PropertyPlaceholderAutoConfiguration.class,
            ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        PerformanceCounterContainer counterContainer = context.getBean(PerformanceCounterContainer.class);
        Field field = counterContainer.getClass().getDeclaredField("performanceCounters");
        field.setAccessible(true);
        Map<String, PerformanceCounter> map = (Map<String, PerformanceCounter>)field.get(counterContainer);
        assertThat(map.containsKey("java.lang:type=ClassLoading")).isNotNull();
    }

    @Test
    public void shouldBeAbleToConfigureJvmPerformanceCounters() {
        EnvironmentTestUtils.addEnvironment(context,
            "azure.application-insights.instrumentation-key: 00000000-0000-0000-0000-000000000000",
            "azure.application-insights.default.modules.JvmPerformanceCountersModule.enabled=true");
        context.register(PropertyPlaceholderAutoConfiguration.class,
            ApplicationInsightsTelemetryAutoConfiguration.class);
        context.refresh();

        assertThat(context.getBeansOfType(JvmPerformanceCountersModule.class)).isNotEmpty();
    }

    private static class CustomModuleConfiguration {

        @Bean
        public ContextInitializer myContextInitializer() {
            return new ContextInitializer() {
                @Override
                public void initialize(TelemetryContext context) {
                }
            };
        }

        @Bean
        public TelemetryInitializer myTelemetryInitializer() {
            return new TelemetryInitializer() {
                @Override
                public void initialize(Telemetry telemetry) {
                }
            };
        }

        @Bean
        public TelemetryModule myTelemetryModule() {
            return new TelemetryModule() {
                @Override
                public void initialize(TelemetryConfiguration configuration) {
                }
            };
        }

        @Bean
        public TelemetryProcessor myTelemetryProcessor() {
            return new TelemetryProcessor() {
                @Override
                public boolean process(Telemetry telemetry) {
                    return true;
                }
            };
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Method method = TelemetryConfiguration.class.getDeclaredMethod("setActiveAsNull");
        method.setAccessible(true);
        method.invoke(null);
    }
}