// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import javax.annotation.Nullable;

public class MoreJava8BytecodeBridge {

  @Nullable
  public static Span spanFromContextOrNull(Context context) {
    return Span.fromContextOrNull(context);
  }

  private MoreJava8BytecodeBridge() {}
}
