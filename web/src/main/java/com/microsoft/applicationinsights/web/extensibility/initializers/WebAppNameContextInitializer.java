package com.microsoft.applicationinsights.web.extensibility.initializers;

import com.microsoft.applicationinsights.common.CommonUtils;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.context.CloudContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;

public class WebAppNameContextInitializer implements ContextInitializer {

    private String appName;

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public void initialize(TelemetryContext context) {
        if (!CommonUtils.isNullOrEmpty(appName)) {
            CloudContext cloud = context.getCloud();
            cloud.setRole(appName);
        }
    }
}
