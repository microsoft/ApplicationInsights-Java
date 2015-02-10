/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.internal.util;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Properties;

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
            InternalLogger.INSTANCE.error("Failed to get canonical host name, exception: %s", e.getMessage());
        }
        return null;
    }

    public static String getLocale()
    {
        final String languageTagMethodName = "toLanguageTag";
        Locale defaultLocale = Locale.getDefault();
        try {
            return (String)Locale.class.getMethod(languageTagMethodName).invoke(defaultLocale);
        } catch (Exception e) {
            // Just log - we'll handle it in the fallback path below
            InternalLogger.INSTANCE.trace("Method '%s' could not be found in Locale class - moving to fallback path.", languageTagMethodName);
        }

        final String localeFileName = "locales.properties";
        Properties localesMap = null;
        try {
            localesMap = PropertyHelper.getProperties(localeFileName);
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Could not find locale mapping file '%s'", localeFileName);
        }

        String localeString = defaultLocale.toString();
        String localeTag = localesMap != null ?  localesMap.getProperty(localeString) : null;
        return localeTag != null ? localeTag : localeString;
    }
}
