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

package com.azure.monitor.opentelemetry.exporter;

import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.test.TestBase;
import com.azure.core.test.TestMode;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

public class AzureMonitorTraceExporterTestBase extends TestBase {

  @Override
  @BeforeEach
  public void setupTest(TestInfo testInfo) {
    Assumptions.assumeFalse(getTestMode() == TestMode.PLAYBACK, "Skipping playback tests");
  }

  @Override
  @AfterEach
  public void teardownTest(TestInfo testInfo) {
    GlobalOpenTelemetry.resetForTest();
  }

  Tracer configureAzureMonitorExporter(HttpPipelinePolicy validator) {
    AzureMonitorTraceExporter exporter =
        new AzureMonitorExporterBuilder()
            .connectionString(System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING"))
            .addHttpPipelinePolicy(validator)
            .buildTraceExporter();

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();

    OpenTelemetrySdk openTelemetrySdk =
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal();
    return openTelemetrySdk.getTracer("Sample");
  }
}
