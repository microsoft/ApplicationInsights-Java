// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;

public class LazyConfiguration {

  public String connectionString;

  public Configuration.Sampling sampling = new Configuration.Sampling();

  public Configuration.Role role = new Configuration.Role();

  public String instrumentationLoggingLevel;

  public String selfDiagnosticsLevel;
}
