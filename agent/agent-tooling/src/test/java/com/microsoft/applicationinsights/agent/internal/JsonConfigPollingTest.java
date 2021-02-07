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

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import com.google.common.io.Resources;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.sampling.AiSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Span.Kind;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static org.junit.Assert.*;

public class JsonConfigPollingTest {

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    @AfterClass
    public static void tearDown() {
        // need to reset trace config back to default (with default sampler)
        // otherwise tests run after this can fail
        DelegatingSampler.getInstance().setDelegate(new AiSampler(100));
    }

    @Test
    public void shouldUpdate() {
        // given
        Configuration lastReadConfiguration = new Configuration();
        lastReadConfiguration.connectionString = "InstrumentationKey=00000000-0000-0000-0000-000000000000";
        lastReadConfiguration.sampling.percentage = 90;

        // when
        Path path = new File(Resources.getResource("applicationinsights.json").getPath()).toPath();
        JsonConfigPolling jsonConfigPolling = new JsonConfigPolling(path, 0, lastReadConfiguration);
        jsonConfigPolling.run();

        // then
        assertEquals("InstrumentationKey=11111111-1111-1111-1111-111111111111", TelemetryConfiguration.getActive().getConnectionString());
        assertEquals(2, getCurrentItemCount());
    }

    @Test
    public void shouldNotUpdate() {
        // given
        Configuration lastReadConfiguration = new Configuration();
        lastReadConfiguration.connectionString = "InstrumentationKey=00000000-0000-0000-0000-000000000000";
        envVars.set("APPLICATIONINSIGHTS_CONNECTION_STRING", "InstrumentationKey=00000000-0000-0000-0000-000000000000");
        envVars.set("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", "90");

        // when
        Path path = new File(Resources.getResource("applicationinsights.json").getPath()).toPath();
        JsonConfigPolling jsonConfigPolling = new JsonConfigPolling(path, 0, lastReadConfiguration);
        jsonConfigPolling.run();

        // then
        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", TelemetryConfiguration.getActive().getConnectionString());
        assertEquals(1, getCurrentItemCount());
    }

    private int getCurrentItemCount() {
        SpanContext spanContext = SpanContext.create(
                "12341234123412341234123412341234",
                "1234123412341234",
                TraceFlags.getDefault(),
                TraceState.getDefault());
        Context parentContext = Context.root().with(Span.wrap(spanContext));
        SamplingResult samplingResult =
                DelegatingSampler.getInstance().shouldSample(parentContext, "12341234123412341234123412341234", "my span name",
                        Kind.SERVER, Attributes.empty(), Collections.emptyList());
        TraceState traceState = samplingResult.getUpdatedTraceState(TraceState.getDefault());
        return Integer.parseInt(traceState.get("ai.internal.item_count"));
    }
}
