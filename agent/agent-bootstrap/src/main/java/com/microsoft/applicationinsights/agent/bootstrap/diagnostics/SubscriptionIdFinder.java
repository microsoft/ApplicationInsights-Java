// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

public class SubscriptionIdFinder extends CachedDiagnosticsValueFinder {

  // visible for testing
  static final String WEBSITE_OWNER_NAME_ENV_VAR = "WEBSITE_OWNER_NAME";

  @Override
  protected String populateValue() {
    String envValue = System.getenv(WEBSITE_OWNER_NAME_ENV_VAR);
    if (envValue == null || envValue.isEmpty()) {
      return "unknown";
    }
    int index = envValue.indexOf('+');
    if (index < 0) {
      return "unknown";
    }
    return envValue.substring(0, index);
  }

  @Override
  public String getName() {
    return "subscriptionId";
  }
}
