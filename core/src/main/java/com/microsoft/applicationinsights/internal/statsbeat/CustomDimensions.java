/*
 * ApplicationInsights-Java
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

package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.*;

public final class CustomDimensions {

    private static CustomDimensions instance;
    private String resourceProvider;
    private String operatingSystem;
    private String customerIkey;
    private String version;
    private String runtimeVersion;
    private ConcurrentMap<String, String> properties;
    private static final Object lock = new Object();
    
    public static CustomDimensions getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new CustomDimensions();
                }
            }
        }
        return instance;
    }
    
    private CustomDimensions() {
        initialize();
    }

    private void initialize() {
        String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();
        if (sdkVersion.startsWith("awr")) {
            resourceProvider = RP_APPSVC;
            operatingSystem = OS_WINDOWS;
        } else if (sdkVersion.startsWith("alr")) {
            resourceProvider = RP_APPSVC;
            operatingSystem = OS_LINUX;
        } else if (sdkVersion.startsWith("kwr")) {
            resourceProvider = RP_AKS;
            operatingSystem = OS_WINDOWS;
        } else if (sdkVersion.startsWith("klr")) {
            resourceProvider = RP_AKS;
            operatingSystem = OS_LINUX;
        } else if (sdkVersion.startsWith("fwr")) {
            resourceProvider = RP_FUNCTIONS;
            operatingSystem = OS_WINDOWS;
        } else if (sdkVersion.startsWith("flr")) {
            resourceProvider = RP_FUNCTIONS;
            operatingSystem = OS_LINUX;
        } else if (sdkVersion.startsWith(LANGUAGE)) {
            resourceProvider = UNKNOWN;
        }

        if (operatingSystem == null) {
            if (SystemInformation.INSTANCE.isWindows()) {
                operatingSystem = OS_WINDOWS;
            } else if (SystemInformation.INSTANCE.isUnix()) {
                operatingSystem = OS_LINUX;
            } else {
                operatingSystem = OS_UNKNOW;
            }
        }

        customerIkey = TelemetryConfiguration.getActive().getInstrumentationKey();
        version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);
        runtimeVersion = System.getProperty("java.version");
        
        properties = new ConcurrentHashMap<>();
        properties.put(CUSTOM_DIMENSIONS_RP, resourceProvider);
        properties.put(CUSTOM_DIMENSIONS_ATTACH_TYPE, ATTACH_TYPE_CODELESS);
        if (customerIkey != null) { // Unit test
            properties.put(CUSTOM_DIMENSIONS_CIKEY, customerIkey);
        }
        properties.put(CUSTOM_DIMENSIONS_RUNTIME_VERSION, runtimeVersion);
        System.out.println("OperatingSystem: " + operatingSystem);
        properties.put(CUSTOM_DIMENSIONS_OS, operatingSystem);
        properties.put(CUSTOM_DIMENSIONS_LANGUAGE, LANGUAGE);
        properties.put(CUSTOM_DIMENSIONS_VERSION, version);
    }

    public ConcurrentMap<String, String> getProperties() {
        return properties;
    }

    public static synchronized void reset() {
        instance = null;
    }
}
