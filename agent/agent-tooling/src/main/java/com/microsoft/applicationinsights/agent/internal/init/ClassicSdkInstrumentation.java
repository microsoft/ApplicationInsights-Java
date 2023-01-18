// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil;
import com.microsoft.applicationinsights.agent.internal.classicsdk.ApplicationInsightsAppenderClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.BytecodeUtilImpl;
import com.microsoft.applicationinsights.agent.internal.classicsdk.ConnectionStringClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.DependencyTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.ExceptionTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.HeartBeatModuleClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.MetricTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.PerformanceCounterModuleClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.QuickPulseClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.RequestNameHandlerClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.RequestTelemetryClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.TelemetryClientClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.TelemetryContextClassFileTransformer;
import com.microsoft.applicationinsights.agent.internal.classicsdk.WebRequestTrackingFilterClassFileTransformer;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import java.lang.instrument.Instrumentation;

class ClassicSdkInstrumentation {

  static void registerTransformers() {
    Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();

    // add sdk instrumentation after ensuring Global.getTelemetryClient() will not return null
    instrumentation.addTransformer(new TelemetryClientClassFileTransformer());
    instrumentation.addTransformer(new DependencyTelemetryClassFileTransformer());
    instrumentation.addTransformer(new RequestTelemetryClassFileTransformer());
    instrumentation.addTransformer(new ExceptionTelemetryClassFileTransformer());
    instrumentation.addTransformer(new MetricTelemetryClassFileTransformer());
    instrumentation.addTransformer(new TelemetryContextClassFileTransformer());
    instrumentation.addTransformer(new PerformanceCounterModuleClassFileTransformer());
    instrumentation.addTransformer(new QuickPulseClassFileTransformer());
    instrumentation.addTransformer(new HeartBeatModuleClassFileTransformer());
    instrumentation.addTransformer(new ApplicationInsightsAppenderClassFileTransformer());
    instrumentation.addTransformer(new WebRequestTrackingFilterClassFileTransformer());
    instrumentation.addTransformer(new RequestNameHandlerClassFileTransformer());
    instrumentation.addTransformer(new DuplicateAgentClassFileTransformer());
    instrumentation.addTransformer(new ConnectionStringClassFileTransformer());

    // this is currently used by Micrometer instrumentation in addition to 2.x SDK
    BytecodeUtil.setDelegate(new BytecodeUtilImpl());
  }

  private ClassicSdkInstrumentation() {}
}
