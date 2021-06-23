package com.microsoft.applicationinsights.smoketest.docker;

import com.google.common.base.MoreObjects;
import com.microsoft.applicationinsights.smoketest.DependencyContainer;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


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
    public boolean equals(Object obj) {
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
