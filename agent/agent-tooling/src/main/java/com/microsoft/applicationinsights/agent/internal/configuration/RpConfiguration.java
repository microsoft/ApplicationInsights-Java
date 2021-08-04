/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.Role;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.Sampling;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RpConfiguration {

  // Use private and add the @JsonIgnore annotation on the getter of the configPath,
  // and enable Jackson deserialization for the field by applying the @JsonProperty annotation on
  // the setter.
  private Path configPath;

  // Use private and add the @JsonIgnore annotation on the getter of the configPath,
  // and enable Jackson deserialization for the field by applying the @JsonProperty annotation on
  // the setter.
  private long lastModifiedTime;

  public String connectionString;

  // intentionally null, so that we can tell if rp is providing or not
  public Sampling sampling = new Sampling();

  // this is needed in Azure Spring Cloud because it will set the role name to application name
  // on behalf of customers by default.
  // Note the role doesn't support hot load due to unnecessary currently.
  public Role role = new Role();

  // this is needed in Azure Functions because .NET SDK always propagates trace flags "00" (not
  // sampled)
  // null means do not override the users selection
  public @Nullable Boolean ignoreRemoteParentNotSampled;

  @JsonIgnore
  public Path getConfigPath() {
    return configPath;
  }

  @JsonProperty
  public void setConfigPath(Path configPath) {
    this.configPath = configPath;
  }

  @JsonIgnore
  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  @JsonProperty
  public void setLastModifiedTime(long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }
}
