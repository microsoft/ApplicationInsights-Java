package com.microsoft.applicationinsights.internal.util;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A view into the context information specific to device information.
 */
public class DeviceInfo
{
    public static String getOperatingSystem()
    {
        java.lang.management.OperatingSystemMXBean osb = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        String version = osb.getVersion();
        String name = osb.getName();
        return name + " " + version;
    }

    public static String getHostName()
    {
        InetAddress ip;
        try
        {
            ip = InetAddress.getLocalHost();
            return ip.getHostName();
        }
        catch (UnknownHostException e)
        {
            InternalLogger.INSTANCE.log("Failed get host name, exception: %s", e.getMessage());
        }
        return null;
    }
}
