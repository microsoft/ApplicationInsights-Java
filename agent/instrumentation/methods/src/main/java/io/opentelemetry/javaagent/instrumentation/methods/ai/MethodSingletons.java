// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

// Includes work from:
/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.methods.ai;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.code.CodeAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.code.CodeSpanNameExtractor;
import io.opentelemetry.instrumentation.api.util.ClassAndMethod;

public final class MethodSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.methods";

  private static final Instrumenter<ClassAndMethod, Void> INSTRUMENTER;

  static {
    CodeAttributesGetter<ClassAndMethod> codeAttributesGetter =
        ClassAndMethod.codeAttributesGetter();

    INSTRUMENTER =
        Instrumenter.<ClassAndMethod, Void>builder(
                GlobalOpenTelemetry.get(),
                INSTRUMENTATION_NAME,
                CodeSpanNameExtractor.create(codeAttributesGetter))
            .addAttributesExtractor(CodeAttributesExtractor.create(codeAttributesGetter))
            // START APPLICATION INSIGHTS MODIFICATIONS
            // we emit SERVER spans instead of INTERNAL spans, so that it works well with
            // Application Insights' customInstrumentations feature
            .buildInstrumenter(SpanKindExtractor.alwaysServer());
    // END APPLICATION INSIGHTS MODIFICATIONS
  }

  public static Instrumenter<ClassAndMethod, Void> instrumenter() {
    return INSTRUMENTER;
  }

  private MethodSingletons() {}
}
