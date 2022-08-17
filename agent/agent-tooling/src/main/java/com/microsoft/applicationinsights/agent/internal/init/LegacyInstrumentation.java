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

import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil;
import com.microsoft.applicationinsights.agent.internal.classicsdk.ApplicationInsightsAppenderClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.BytecodeUtilImpl;
import com.microsoft.applicationinsights.agent.internal.classicsdk.DependencyTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.ExceptionTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.HeartBeatModuleClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.PerformanceCounterModuleClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.QuickPulseClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.RequestNameHandlerClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.RequestTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.TelemetryClientClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.WebRequestTrackingFilterClassFileTransformer;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import java.lang.instrument.Instrumentation;

class LegacyInstrumentation {

  static void registerTransformers() {
    Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();

    // add sdk instrumentation after ensuring Global.getTelemetryClient() will not return null
    instrumentation.addTransformer(new TelemetryClientClassFileTransformer());
    instrumentation.addTransformer(new DependencyTelemetryClassFileTransformer());
    instrumentation.addTransformer(new RequestTelemetryClassFileTransformer());
    instrumentation.addTransformer(new ExceptionTelemetryClassFileTransformer());
    instrumentation.addTransformer(new PerformanceCounterModuleClassFileTransformer());
    instrumentation.addTransformer(new QuickPulseClassFileTransformer());
    instrumentation.addTransformer(new HeartBeatModuleClassFileTransformer());
    instrumentation.addTransformer(new ApplicationInsightsAppenderClassFileTransformer());
    instrumentation.addTransformer(new WebRequestTrackingFilterClassFileTransformer());
    instrumentation.addTransformer(new RequestNameHandlerClassFileTransformer());
    instrumentation.addTransformer(new DuplicateAgentClassFileTransformer());

    // this is currently used by Micrometer instrumentation in addition to 2.x SDK
    BytecodeUtil.setDelegate(new BytecodeUtilImpl());
  }

  private LegacyInstrumentation() {}
}
