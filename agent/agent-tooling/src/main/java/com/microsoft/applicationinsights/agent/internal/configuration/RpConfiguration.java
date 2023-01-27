// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.Role;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.Sampling;
import java.nio.file.Path;

public class RpConfiguration {

  @JsonIgnore public Path configPath;

  @JsonIgnore public long lastModifiedTime;

  public String connectionString;

  public Sampling sampling = new Sampling();

  // this is needed in Azure Spring Cloud because it will set the role name to application name
  // on behalf of customers by default.
  // Note the role doesn't support hot load currently.
  public Role role = new Role();
}
