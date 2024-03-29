// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.sdk.metrics.internal.view;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.SemanticAttributes;

final class UserAgents {

  static boolean isBot(Attributes attributes) {
    String userAgent = attributes.get(SemanticAttributes.USER_AGENT_ORIGINAL);
    return userAgent != null && userAgent.contains("AlwaysOn");
  }

  private UserAgents() {}
}
