// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.instrumentation.api.instrumenter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

public final class UserAgents {

  public static boolean isBot(Attributes endAttributes, Attributes startAttributes) {
    String userAgent = endAttributes.get(SemanticAttributes.HTTP_USER_AGENT);
    if (userAgent == null) {
      userAgent = startAttributes.get(SemanticAttributes.HTTP_USER_AGENT);
    }
    return userAgent != null && userAgent.contains("AlwaysOn");
  }

  private UserAgents() {}
}
