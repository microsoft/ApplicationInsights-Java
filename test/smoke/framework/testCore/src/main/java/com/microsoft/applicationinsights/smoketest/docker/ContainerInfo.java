package com.microsoft.applicationinsights.smoketest.docker;

import com.google.common.base.MoreObjects;
import com.microsoft.applicationinsights.smoketest.DependencyContainer;


public class ContainerInfo {
    private final String containerId;
    private final String imageName;
    private String containerName;
    private DependencyContainer dependencyContainerInfo;

    public ContainerInfo(String containerId, String imageName) {
        this.containerId = containerId;
        this.imageName = imageName;
    }
    /**
     * @return the containerId
     */
    public String getContainerId() {
        return containerId;
    }
    /**
     * @return the imageName
     */
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
    public String toString() {
        return MoreObjects.toStringHelper(ContainerInfo.class)
            .add("id", getContainerId())
            .add("image", getImageName())
            .add("containerName", getContainerName())
            .add("isDepdencnecy", isDependency())
            .toString();
    }
}
