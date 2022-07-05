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

package com.microsoft.applicationinsights.agent.internal.init;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

@AutoService(AgentListener.class)
public class AfterAgentListener implements AgentListener {

  private static volatile AppIdSupplier appIdSupplier;

  public static void setAppIdSupplier(AppIdSupplier appIdSupplier) {
    AfterAgentListener.appIdSupplier = appIdSupplier;
  }

  @Override
  public void afterAgent(
      Config config, AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    // only safe now to resolve app id because SSL initialization
    // triggers loading of java.util.logging (starting with Java 8u231)
    // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized.

    if (!"java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
      // Delay registering and starting AppId retrieval until the connection string becomes
      // available for Linux Consumption Plan.
      appIdSupplier.startAppIdRetrieval();
    }

    LazyHttpClient.safeToInitLatch.countDown();

    PerformanceCounterInitializer.initialize(FirstEntryPoint.getConfiguration());
  }
}
