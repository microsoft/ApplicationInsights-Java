/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import javax.annotation.Nullable;

public class MoreJava8BytecodeBridge {

  @Nullable
  public static Span spanFromContextOrNull(Context context) {
    return Span.fromContextOrNull(context);
  }
}
