package com.microsoft.applicationinsights.extensibility.initializer;

import com.microsoft.applicationinsights.common.CommonUtils;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.context.CloudContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;

/**
 * Initializes cloud context information.
 * Currently setting RoleInstance to the hostname.
 */
public final class CloudInfoContextInitializer implements ContextInitializer {
    @Override
    public void initialize(TelemetryContext context) {
        CloudContext cloud = context.getCloud();
        String hostName = CommonUtils.getHostName();
        if (!CommonUtils.isNullOrEmpty(hostName)) {
            cloud.setRoleInstance(hostName);
        }

    }
}
