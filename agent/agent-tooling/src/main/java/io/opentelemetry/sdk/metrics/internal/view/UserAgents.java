// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.sdk.metrics.internal.view;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

public final class UserAgents {

  public static boolean isBot(Attributes attributes) {
    String userAgent = attributes.get(SemanticAttributes.HTTP_USER_AGENT);
    return userAgent != null && userAgent.contains("AlwaysOn");
  }

  private UserAgents() {}
}
