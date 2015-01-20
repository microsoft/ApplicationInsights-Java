package com.microsoft.applicationinsights.internal.util;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A view into the context information specific to device information.
 */
public class DeviceInfo
{
    private static OperatingSystemMXBean osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();

    public static String getOperatingSystem()
    {
        return osBean.getName();
    }

    public static String getOperatingSystemVersion()
    {
        // Note: osBean.getName will return a string like "Windows 8.1" which should be good enough for this field.
        // Calling osBean.getVersion on the other hand will only return 6.3 (Windows NT version) which will be less
        // intuitive to customers.
        return osBean.getName();
    }

    public static String getOperatingVersionArchitecture()
    {
        return osBean.getArch();
    }

    public static String getHostName()
    {
        InetAddress ip;
        try
        {
            ip = InetAddress.getLocalHost();
            return ip.getCanonicalHostName();
        }
        catch (UnknownHostException e)
        {
            InternalLogger.INSTANCE.log("Failed to get canonical host name, exception: %s", e.getMessage());
        }
        return null;
    }
}
