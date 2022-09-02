// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

public class AgentExtensionVersionFinder extends CachedDiagnosticsValueFinder {

  /**
   * Follows variable naming scheme for extension versions: https://github
   * .com/projectkudu/kudu/wiki/Azure-Site-Extensions#pre-installed-site-extensions
   */
  public static final String AGENT_EXTENSION_VERSION_ENVIRONMENT_VARIABLE =
      "ApplicationInsightsAgent_EXTENSION_VERSION";

  @Override
  protected String populateValue() {
    return System.getenv(AGENT_EXTENSION_VERSION_ENVIRONMENT_VARIABLE);
  }

  @Override
  public String getName() {
    return "extensionVersion";
  }
}
