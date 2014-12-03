package com.microsoft.applicationinsights.implementation;

import com.microsoft.applicationinsights.datacontracts.TelemetryContext;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.util.DeviceInfo;

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
        device.setId(DeviceInfo.getHostName());
        device.setLanguage(Locale.getDefault().toLanguageTag());
    }
}
