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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.*;

class CustomDimensions {

    private static final CustomDimensions instance = new CustomDimensions();

    private volatile ResourceProvider resourceProvider;

    private final OperatingSystem operatingSystem;

    private final ConcurrentMap<String, String> properties;

    static CustomDimensions get() {
        return instance;
    }

    // visible for testing
    CustomDimensions() {
        String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();

        if (sdkVersion.startsWith("awr")) {
            resourceProvider = ResourceProvider.RP_APPSVC;
            operatingSystem = OperatingSystem.OS_WINDOWS;
        } else if (sdkVersion.startsWith("alr")) {
            resourceProvider = ResourceProvider.RP_APPSVC;
            operatingSystem = OperatingSystem.OS_LINUX;
        } else if (sdkVersion.startsWith("kwr")) {
            resourceProvider = ResourceProvider.RP_AKS;
            operatingSystem = OperatingSystem.OS_WINDOWS;
        } else if (sdkVersion.startsWith("klr")) {
            resourceProvider = ResourceProvider.RP_AKS;
            operatingSystem = OperatingSystem.OS_LINUX;
        } else if (sdkVersion.startsWith("fwr")) {
            resourceProvider = ResourceProvider.RP_FUNCTIONS;
            operatingSystem = OperatingSystem.OS_WINDOWS;
        } else if (sdkVersion.startsWith("flr")) {
            resourceProvider = ResourceProvider.RP_FUNCTIONS;
            operatingSystem = OperatingSystem.OS_LINUX;
        } else {
            resourceProvider = ResourceProvider.UNKNOWN;
            operatingSystem = initOperatingSystem();
        }

        String customerIkey = TelemetryConfiguration.getActive().getInstrumentationKey();
        String version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);
        String runtimeVersion = System.getProperty("java.version");

        properties = new ConcurrentHashMap<>();
        properties.put(CUSTOM_DIMENSIONS_ATTACH_TYPE, ATTACH_TYPE_CODELESS);
        if (customerIkey != null) { // Unit test
            properties.put(CUSTOM_DIMENSIONS_CIKEY, customerIkey);
        }
        properties.put(CUSTOM_DIMENSIONS_RUNTIME_VERSION, runtimeVersion);
        properties.put(CUSTOM_DIMENSIONS_LANGUAGE, LANGUAGE);
        properties.put(CUSTOM_DIMENSIONS_VERSION, version);
    }

    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public void setResourceProvider(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    // TODO replace with individual getters
    String getProperty(String key) {
        return properties.get(key);
    }

    // TODO replace with individual getters
    void updateProperty(String key, String value) {
        properties.put(key, value);
    }

    void populateProperties(Map<String, String> properties) {
        properties.putAll(this.properties);
        properties.put(CUSTOM_DIMENSIONS_RP, resourceProvider.toString());
        properties.put(CUSTOM_DIMENSIONS_OS, operatingSystem.toString());
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
