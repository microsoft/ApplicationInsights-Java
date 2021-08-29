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

package com.microsoft.applicationinsights.smoketest.docker;

import com.google.common.base.MoreObjects;
import com.microsoft.applicationinsights.smoketest.DependencyContainer;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ContainerInfo {
  private final String containerId;
  private final String imageName;
  private String containerName;
  private DependencyContainer dependencyContainerInfo;

  public ContainerInfo(String containerId, String imageName) {
    this.containerId = containerId;
    this.imageName = imageName;
  }

  public String getContainerId() {
    return containerId;
  }

  public String getImageName() {
    return imageName;
  }

  public String getContainerName() {
    return containerName;
  }

  public void setContainerName(String containerName) {
    this.containerName = containerName;
  }

  public void setDependencyContainerInfo(DependencyContainer dc) {
    this.dependencyContainerInfo = dc;
  }

  public DependencyContainer getDependencyContainerInfo() {
    return dependencyContainerInfo;
  }

  public boolean isDependency() {
    return this.dependencyContainerInfo != null;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ContainerInfo)) {
      return false;
    }

    ContainerInfo that = (ContainerInfo) obj;

    return new EqualsBuilder()
        .append(getContainerId(), that.getContainerId())
        .append(getImageName(), that.getImageName())
        .append(getContainerName(), that.getContainerName())
        .append(getDependencyContainerInfo(), that.getDependencyContainerInfo())
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(getContainerId())
        .append(getImageName())
        .append(getContainerName())
        .append(getDependencyContainerInfo())
        .toHashCode();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(ContainerInfo.class)
        .add("id", getContainerId())
        .add("image", getImageName())
        .add("containerName", getContainerName())
        .add("isDepdencnecy", isDependency())
        .toString();
  }
}
