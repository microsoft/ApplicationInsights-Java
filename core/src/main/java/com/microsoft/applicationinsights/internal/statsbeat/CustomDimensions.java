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

import java.util.Map;

class CustomDimensions {

    private static final CustomDimensions instance = new CustomDimensions();

    private volatile ResourceProvider resourceProvider;
    private volatile OperatingSystem operatingSystem;

    private final String attachType;
    private final String customerIkey;
    private final String runtimeVersion;
    private final String language;
    private final String sdkVersion;

    static CustomDimensions get() {
        return instance;
    }

    // visible for testing
    CustomDimensions() {
        String qualifiedSdkVersion = PropertyHelper.getQualifiedSdkVersionString();

        if (qualifiedSdkVersion.startsWith("awr")) {
            resourceProvider = ResourceProvider.RP_APPSVC;
            operatingSystem = OperatingSystem.OS_WINDOWS;
        } else if (qualifiedSdkVersion.startsWith("alr")) {
            resourceProvider = ResourceProvider.RP_APPSVC;
            operatingSystem = OperatingSystem.OS_LINUX;
        } else if (qualifiedSdkVersion.startsWith("kwr")) {
            resourceProvider = ResourceProvider.RP_AKS;
            operatingSystem = OperatingSystem.OS_WINDOWS;
        } else if (qualifiedSdkVersion.startsWith("klr")) {
            resourceProvider = ResourceProvider.RP_AKS;
            operatingSystem = OperatingSystem.OS_LINUX;
        } else if (qualifiedSdkVersion.startsWith("fwr")) {
            resourceProvider = ResourceProvider.RP_FUNCTIONS;
            operatingSystem = OperatingSystem.OS_WINDOWS;
        } else if (qualifiedSdkVersion.startsWith("flr")) {
            resourceProvider = ResourceProvider.RP_FUNCTIONS;
            operatingSystem = OperatingSystem.OS_LINUX;
        } else {
            resourceProvider = ResourceProvider.UNKNOWN;
            operatingSystem = initOperatingSystem();
        }

        customerIkey = TelemetryConfiguration.getActive().getInstrumentationKey();
        sdkVersion = qualifiedSdkVersion.substring(qualifiedSdkVersion.lastIndexOf(':') + 1);
        runtimeVersion = System.getProperty("java.version");

        attachType = "codeless";
        language = "java";
    }

    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public String getCustomerIkey() { return customerIkey; }

    public void setResourceProvider(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public void setOperatingSystem(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    void populateProperties(Map<String, String> properties) {
        properties.put("rp", resourceProvider.getValue());
        properties.put("os", operatingSystem.getValue());
        properties.put("attach", attachType);
        properties.put("cikey", customerIkey);
        properties.put("runtimeVersion", runtimeVersion);
        properties.put("language", language);
        properties.put("version", sdkVersion);
    }

    private static OperatingSystem initOperatingSystem() {
        if (SystemInformation.INSTANCE.isWindows()) {
            return OperatingSystem.OS_WINDOWS;
        } else if (SystemInformation.INSTANCE.isUnix()) {
            return OperatingSystem.OS_LINUX;
        } else {
            return OperatingSystem.OS_UNKNOWN;
        }
    }
}
