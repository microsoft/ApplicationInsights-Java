// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.sdk.metrics.internal.view;

import com.microsoft.applicationinsights.agent.internal.init.AiContextKeys;
import io.opentelemetry.context.Context;

final class UserAgents {

  static boolean isBot(Context context) {
    String userAgent = context.get(AiContextKeys.USER_AGENT);
    return userAgent != null && userAgent.contains("AlwaysOn");
  }

  private UserAgents() {}
}
