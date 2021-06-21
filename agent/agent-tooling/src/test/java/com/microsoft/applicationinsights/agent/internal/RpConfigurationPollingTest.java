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

package com.microsoft.applicationinsights.agent.internal;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryUtil;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.RpConfiguration;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemStubsExtension.class)
class RpConfigurationPollingTest {

    @SystemStub
    EnvironmentVariables envVars = new EnvironmentVariables();

    @BeforeEach
    void beforeEach() {
        // default sampler at startup is "Sampler.alwaysOff()", and this test relies on real sampler
        DelegatingSampler.getInstance().setDelegate(Samplers.getSampler(100, new Configuration()));
    }

    @AfterEach
    void afterEach() {
        // need to reset trace config back to default (with default sampler)
        // otherwise tests run after this can fail
        DelegatingSampler.getInstance().setDelegate(Samplers.getSampler(100, new Configuration()));
    }

    @Test
    void shouldUpdate() throws URISyntaxException {
        // given
        RpConfiguration rpConfiguration = new RpConfiguration();
        rpConfiguration.connectionString = "InstrumentationKey=11111111-1111-1111-1111-111111111111";
        rpConfiguration.sampling.percentage = 90;
        rpConfiguration.configPath = Paths.get(RpConfigurationPollingTest.class.getResource("/applicationinsights-rp.json").toURI());
        rpConfiguration.lastModifiedTime = 0;

        TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000");
        Global.setSamplingPercentage(100);

        // pre-check
        assertThat(telemetryClient.getConnectionString()).isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
        assertThat(Global.getSamplingPercentage()).isEqualTo(100);
        assertThat(getCurrentSamplingPercentage()).isEqualTo(100);

        // when
        new RpConfigurationPolling(rpConfiguration, new Configuration(), telemetryClient).run();

        // then
        assertThat(telemetryClient.getConnectionString()).isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
        assertThat(Global.getSamplingPercentage()).isEqualTo(10);
        assertThat(getCurrentSamplingPercentage()).isEqualTo(10);
    }

    @Test
    void shouldUpdateEvenOverEnvVars() throws URISyntaxException {
        // given
        RpConfiguration rpConfiguration = new RpConfiguration();
        rpConfiguration.connectionString = "InstrumentationKey=11111111-1111-1111-1111-111111111111";
        rpConfiguration.sampling.percentage = 90;
        rpConfiguration.configPath = Paths.get(RpConfigurationPollingTest.class.getResource("/applicationinsights-rp.json").toURI());
        rpConfiguration.lastModifiedTime = 0;

        TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setConnectionString("InstrumentationKey=00000000-0000-0000-0000-000000000000");
        Global.setSamplingPercentage(100);

        envVars.set("APPLICATIONINSIGHTS_CONNECTION_STRING", "InstrumentationKey=00000000-0000-0000-0000-000000000000");
        envVars.set("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", "90");

        // pre-check
        assertThat(telemetryClient.getConnectionString()).isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
        assertThat(Global.getSamplingPercentage()).isEqualTo(100);
        assertThat(getCurrentSamplingPercentage()).isEqualTo(100);

        // when
        new RpConfigurationPolling(rpConfiguration, new Configuration(), telemetryClient).run();

        // then
        assertThat(telemetryClient.getConnectionString()).isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
        assertThat(Global.getSamplingPercentage()).isEqualTo(10);
        assertThat(getCurrentSamplingPercentage()).isEqualTo(10);
    }

    private static double getCurrentSamplingPercentage() {
        SpanContext spanContext = SpanContext.create(
                "12341234123412341234123412341234",
                "1234123412341234",
                TraceFlags.getSampled(),
                TraceState.getDefault());
        Context parentContext = Context.root().with(Span.wrap(spanContext));
        SamplingResult samplingResult =
                DelegatingSampler.getInstance().shouldSample(parentContext, "12341234123412341234123412341234", "my span name",
                        SpanKind.SERVER, Attributes.empty(), Collections.emptyList());
        TraceState traceState = samplingResult.getUpdatedTraceState(TraceState.getDefault());
        return Double.parseDouble(traceState.get(TelemetryUtil.SAMPLING_PERCENTAGE_TRACE_STATE));
    }
}
