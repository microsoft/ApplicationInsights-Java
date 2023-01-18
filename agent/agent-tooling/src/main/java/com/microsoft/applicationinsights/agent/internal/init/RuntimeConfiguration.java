// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import java.util.ArrayList;
import java.util.List;

public class RuntimeConfiguration {

  public String connectionString;
  public Configuration.Role role = new Configuration.Role();

  public Configuration.Sampling sampling = new Configuration.Sampling();
  public Configuration.SamplingPreview samplingPreview = new Configuration.SamplingPreview();

  public boolean propagationDisabled;
  public List<String> additionalPropagators = new ArrayList<>();
  public boolean legacyRequestIdPropagationEnabled;

  public String instrumentationLoggingLevel;

  public String selfDiagnosticsLevel;
}
