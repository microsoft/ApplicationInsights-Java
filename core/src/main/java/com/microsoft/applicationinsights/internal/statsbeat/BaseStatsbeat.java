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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.*;

public abstract class BaseStatsbeat {

    private static final Logger logger = LoggerFactory.getLogger(BaseStatsbeat.class);
    protected String resourceProvider;
    protected String operatingSystem;
    protected TelemetryClient telemetryClient;
    protected ScheduledExecutorService scheduledExecutor;
    protected long interval;

    private String customerIkey;
    private String version;
    private String runtimeVersion;

    public BaseStatsbeat() {
        initializeCommonProperties();
        interval = DEFAULT_STATSBEAT_INTERVAL;
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(BaseStatsbeat.class));
        scheduledExecutor.scheduleAtFixedRate(sendStatsbeat(), interval, interval, TimeUnit.SECONDS);
    }

    /**
     * @return the name of the resource provider
     */
    public String getResourceProvider() {
        return resourceProvider;
    }

    /**
     * @return the customer's iKey
     */
    public String getCustomerIkey() {
        return customerIkey;
    }

    /**
     * @return the operating system of the service or application that is being instrumented.
     */
    public String getOperatingSystem() {
        return operatingSystem;
    }

    /**
     * @return the version of the Java Codeless Agent
     */
    public String getVersion() {
        return version;
    }

    private void initializeCommonProperties() {
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
    }

    protected abstract void send(TelemetryClient telemetryClient);

    protected abstract void reset();

    protected StatsbeatTelemetry createStatsbeatTelemetry(String name, double value) {
        StatsbeatTelemetry statsbeatTelemetry = new StatsbeatTelemetry(name, value);
        statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_RP, resourceProvider);
        statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_ATTACH_TYPE, ATTACH_TYPE_CODELESS);
        statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_CIKEY, customerIkey);
        statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_RUNTIME_VERSION, runtimeVersion);
        statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_OS, operatingSystem);
        statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_LANGUAGE, LANGUAGE);
        statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_VERSION, version);
        return statsbeatTelemetry;
    }


    /**
     * Runnable which is responsible for calling the send method to transmit Statsbeat telemetry
     * @return Runnable which has logic to send statsbeat.
     */
    private Runnable sendStatsbeat() {
        return new Runnable() {
            @Override
            public void run() {
                if (telemetryClient == null) {
                    telemetryClient = new TelemetryClient();
                }
                try {
                    send(telemetryClient);
                    logger.debug("#### sending a statsbeat");
                    reset();
                    logger.debug("#### reset counter after each interval");
                }
                catch (Exception e) {
                    logger.error("Error occurred while sending statsbeat");
                }
            }
        };
    }

    protected void updateFrequencyInterval(long newInterval) {
        interval = newInterval;
        scheduledExecutor.scheduleAtFixedRate(sendStatsbeat(), interval, interval, TimeUnit.SECONDS);
    }

    protected long getInterval() {
        return interval;
    }
}