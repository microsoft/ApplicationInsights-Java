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

package com.microsoft.applicationinsights;

import com.azure.monitor.opentelemetry.exporter.implementation.ApplicationInsightsClientImpl;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.config.ApplicationInsightsXmlConfiguration;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString;
import com.microsoft.applicationinsights.internal.config.connection.EndpointProvider;
import com.microsoft.applicationinsights.internal.config.connection.InvalidConnectionStringException;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TelemetryConfiguration {

    // Synchronization for instance initialization
    private static final Object s_lock = new Object();
    private static volatile TelemetryConfiguration active;

    private String instrumentationKey;
    private String connectionString;
    private String roleName;
    private String roleInstance;

    private final EndpointProvider endpointProvider = new EndpointProvider();

    private final Map<String, String> customDimensions;
    private final List<TelemetryModule> telemetryModules = new CopyOnWriteArrayList<>();

    private @Nullable ApplicationInsightsClientImpl channel;

    // only used by tests
    public TelemetryConfiguration() {
        this(new HashMap<>());
    }

    public TelemetryConfiguration(Map<String, String> customDimensions) {
        this.customDimensions = customDimensions;
    }

    /**
     * Gets the active {@link com.microsoft.applicationinsights.TelemetryConfiguration} instance loaded from the
     * ApplicationInsights.xml file. If the configuration file does not exist, the active configuration instance is
     * initialized with minimum defaults needed to send telemetry to Application Insights.
     * @return The 'Active' instance
     */
    public static TelemetryConfiguration getActive() {
        if (active == null) {
            throw new IllegalStateException("agent was not initialized");
        }

        return active;
    }

    /**
     * This method provides the new instance of TelmetryConfiguration without loading the configuration
     * from configuration file. This will just give a plain bare bone instance. Typically used when
     * performing configuration programatically by creating beans, using @Beans tags. This is a common
     * scenario in SpringBoot.
     * @return {@link com.microsoft.applicationinsights.TelemetryConfiguration}
     */
    public static TelemetryConfiguration initActive(Map<String, String> customDimensions, ApplicationInsightsXmlConfiguration applicationInsightsConfig) {
        if (active != null) {
            throw new IllegalStateException("Already initialized");
        }
        if (active == null) {
            synchronized (s_lock) {
                if (active == null) {
                    active = new TelemetryConfiguration(customDimensions);
                    TelemetryConfigurationFactory.INSTANCE.initialize(active, applicationInsightsConfig);
                }
            }
        }
        return active;
    }

    /**
     * Gets the telemetry channel.
     */
    public synchronized ApplicationInsightsClientImpl getChannel() {
        return channel;
    }

    /**
     * Sets the telemetry channel.
     */
    public synchronized void setChannel(ApplicationInsightsClientImpl channel) {
        this.channel = channel;
    }

    // this method only exists for generating bytecode via ASMifier in TelemetryClientClassFileTransformer
    @Deprecated
    public boolean isTrackingDisabled() {
        return true;
    }

    public Map<String, String> getCustomDimensions() {
        return customDimensions;
    }

    public List<TelemetryModule> getTelemetryModules() {
        return telemetryModules;
    }

    /**
     * Gets or sets the default instrumentation key for the application.
     */
    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    /**
     * Gets or sets the default instrumentation key for the application.
     */
    public void setInstrumentationKey(String key) {

        // A non null, non empty instrumentation key is a must
        if (Strings.isNullOrEmpty(key)) {
            throw new IllegalArgumentException("key");
        }

        instrumentationKey = key;
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

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        try {
            ConnectionString.parseInto(connectionString, this);
        } catch (InvalidConnectionStringException e) {
            throw new IllegalArgumentException("Invalid connection string", e);
        }
        this.connectionString = connectionString;
    }

    public EndpointProvider getEndpointProvider() {
        return endpointProvider;
    }

    /**
     * Method for tear down in tests
     */
    @VisibleForTesting
    static void setActiveAsNull() {
        active = null;
    }
}
