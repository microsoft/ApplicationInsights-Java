package com.microsoft.applicationinsights.smoketest.docker;

import com.google.common.base.MoreObjects;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;

public class ContainerInfo {
    private final String containerId;
    private final String imageName;
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
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ContainerInfo.class)
            .add("id", getContainerId())
            .add("image", getImageName())
            .toString();
    }
}
