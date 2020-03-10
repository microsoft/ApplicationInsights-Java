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

package com.microsoft.applicationinsights.internal.config;

/**
 * Created by gupele on 3/13/2015.
 */
public class ApplicationInsightsXmlConfiguration {

    private String instrumentationKey;

    private String connectionString;

    private String roleName;

    private String roleInstance;

    public boolean disableTelemetry;

    private ChannelXmlElement channel = new ChannelXmlElement();

    private TelemetryModulesXmlElement modules;

    private PerformanceCountersXmlElement performance = new PerformanceCountersXmlElement();

    private SDKLoggerXmlElement sdkLogger;

    private QuickPulseXmlElement quickPulse;

    private String schemaVersion;

    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    public void setInstrumentationKey(String instrumentationKey) {
        this.instrumentationKey = instrumentationKey;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleInstance() {
        return roleInstance;
    }

    public void setRoleInstance(String roleInstance) {
        this.roleInstance = roleInstance;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public ChannelXmlElement getChannel() {
        return channel;
    }

    public void setChannel(ChannelXmlElement channel) {
        this.channel = channel;
    }

    public QuickPulseXmlElement getQuickPulse() {
        if (quickPulse == null) {
            quickPulse = new QuickPulseXmlElement();
        }
        return quickPulse;
    }

    public void setQuickPulse(QuickPulseXmlElement quickPulse) {
        this.quickPulse = quickPulse;
    }

    public SDKLoggerXmlElement getSdkLogger() {
        return sdkLogger;
    }

    public void setSdkLogger(SDKLoggerXmlElement sdkLogger) {
        this.sdkLogger = sdkLogger;
    }

    public boolean isDisableTelemetry() {
        return disableTelemetry;
    }

    public void setDisableTelemetry(boolean disableTelemetry) {
        this.disableTelemetry = disableTelemetry;
    }

    public TelemetryModulesXmlElement getModules() {
        return modules;
    }

    public void setModules(TelemetryModulesXmlElement modules) {
        this.modules = modules;
    }

    public PerformanceCountersXmlElement getPerformance() {
        return performance;
    }

    public void setPerformance(PerformanceCountersXmlElement performance) {
        this.performance = performance;
    }
}
