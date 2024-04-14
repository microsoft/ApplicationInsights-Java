// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics;

import java.util.function.Function;

public class AgentExtensionVersionFinder extends CachedDiagnosticsValueFinder {

  /**
   * Follows variable naming scheme for extension versions: https://github
   * .com/projectkudu/kudu/wiki/Azure-Site-Extensions#pre-installed-site-extensions
   */
  public static final String AGENT_EXTENSION_VERSION_ENVIRONMENT_VARIABLE =
      "ApplicationInsightsAgent_EXTENSION_VERSION";

  @Override
  protected String populateValue(Function<String, String> envVarsFunction) {
    return envVarsFunction.apply(AGENT_EXTENSION_VERSION_ENVIRONMENT_VARIABLE);
  }

  @Override
  public String getName() {
    return "extensionVersion";
  }
}
