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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.*;

public abstract class BaseStatsbeat {

    private static final Logger logger = LoggerFactory.getLogger(BaseStatsbeat.class);
    protected final TelemetryClient telemetryClient;
    protected static final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(BaseStatsbeat.class));
    protected final long interval;
    protected static final CommonProperties commonProperties = initializeCommonProperties();

    private final Object lock = new Object();

    public BaseStatsbeat(TelemetryClient telemetryClient, long interval) {
        this.telemetryClient = telemetryClient;
        this.interval = interval;
        scheduledExecutor.scheduleAtFixedRate(sendStatsbeat(), interval, interval, TimeUnit.SECONDS);
    }

    /**
     * @return the name of the resource provider
     */
    public String getResourceProvider() {
        return commonProperties.resourceProvider;
    }

    /**
     * @return the operating system of the service or application that is being instrumented.
     */
    public String getOperatingSystem() {
        return commonProperties.operatingSystem;
    }

    /**
     * @return the version of the Java Codeless Agent
     */
    public String getVersion() {
        return commonProperties.version;
    }

    private static CommonProperties initializeCommonProperties() {
        CommonProperties commonProperties = new CommonProperties();
        String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();
        if (sdkVersion.startsWith("awr")) {
            commonProperties.resourceProvider = RP_APPSVC;
            commonProperties.operatingSystem = OS_WINDOWS;
        } else if (sdkVersion.startsWith("alr")) {
            commonProperties.resourceProvider = RP_APPSVC;
            commonProperties.operatingSystem = OS_LINUX;
        } else if (sdkVersion.startsWith("kwr")) {
            commonProperties.resourceProvider = RP_AKS;
            commonProperties.operatingSystem = OS_WINDOWS;
        } else if (sdkVersion.startsWith("klr")) {
            commonProperties.resourceProvider = RP_AKS;
            commonProperties.operatingSystem = OS_LINUX;
        } else if (sdkVersion.startsWith("fwr")) {
            commonProperties.resourceProvider = RP_FUNCTIONS;
            commonProperties.operatingSystem = OS_WINDOWS;
        } else if (sdkVersion.startsWith("flr")) {
            commonProperties.resourceProvider = RP_FUNCTIONS;
            commonProperties.operatingSystem = OS_LINUX;
        } else if (sdkVersion.startsWith(LANGUAGE)) {
            commonProperties.resourceProvider = UNKNOWN;
        }

        if (commonProperties.operatingSystem == null) {
            if (SystemInformation.INSTANCE.isWindows()) {
                commonProperties.operatingSystem = OS_WINDOWS;
            } else if (SystemInformation.INSTANCE.isUnix()) {
                commonProperties.operatingSystem = OS_LINUX;
            } else {
                commonProperties.operatingSystem = OS_UNKNOW;
            }
        }

        commonProperties.customerIkey = TelemetryConfiguration.getActive().getInstrumentationKey();
        commonProperties.version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);
        commonProperties.runtimeVersion = System.getProperty("java.version");
        return commonProperties;
    }

    protected abstract void send();

    protected MetricTelemetry createStatsbeatTelemetry(String name, double value) {
        MetricTelemetry telemetry = new MetricTelemetry(name, value);
        telemetry.setTelemetryName(STATSBEAT_TELEMETRY_NAME);
        telemetry.getContext().setInstrumentationKey(TelemetryConfiguration.getActive().getStatsbeatInstrumentationKey());
        telemetry.getProperties().put(CUSTOM_DIMENSIONS_RP, commonProperties.resourceProvider);
        telemetry.getProperties().put(CUSTOM_DIMENSIONS_ATTACH_TYPE, ATTACH_TYPE_CODELESS);
        telemetry.getProperties().put(CUSTOM_DIMENSIONS_CIKEY, commonProperties.customerIkey);
        telemetry.getProperties().put(CUSTOM_DIMENSIONS_RUNTIME_VERSION, commonProperties.runtimeVersion);
        telemetry.getProperties().put(CUSTOM_DIMENSIONS_OS, commonProperties.operatingSystem);
        telemetry.getProperties().put(CUSTOM_DIMENSIONS_LANGUAGE, LANGUAGE);
        telemetry.getProperties().put(CUSTOM_DIMENSIONS_VERSION, commonProperties.version);
        return telemetry;
    }

    /**
     * Runnable which is responsible for calling the send method to transmit Statsbeat telemetry
     * @return Runnable which has logic to send statsbeat.
     */
    protected Runnable sendStatsbeat() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    send();
                }
                catch (Exception e) {
                    logger.error("Error occurred while sending statsbeat", e);
                }
            }
        };
    }

    protected long getInterval() {
        return interval;
    }

    static class CommonProperties {
        public String resourceProvider;
        public String operatingSystem;
        public String customerIkey;
        public String version;
        public String runtimeVersion;
    }
}