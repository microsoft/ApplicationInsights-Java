package com.microsoft.applicationinsights.extensibility.initializer;

import com.microsoft.applicationinsights.extensibility.context.DeviceContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.internal.util.DeviceInfo;

import java.util.Locale;

/**
 * Initializer class for device context information.
 */
public class DeviceInfoContextInitializer implements ContextInitializer
{
    @Override
    public void Initialize(TelemetryContext context)
    {
        DeviceContext device = context.getDevice();
        device.setOperatingSystem(DeviceInfo.getOperatingSystem());
        device.setOperatingSystemVersion(DeviceInfo.getOperatingSystemVersion());
        device.setId(DeviceInfo.getHostName());
        device.setLanguage(Locale.getDefault().toString());
    }
}
