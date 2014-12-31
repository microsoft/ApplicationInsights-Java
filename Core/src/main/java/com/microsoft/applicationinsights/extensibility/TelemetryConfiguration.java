package com.microsoft.applicationinsights.extensibility;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.channel.TelemetryChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration data and logic for telemetry clients.
 */
public enum TelemetryConfiguration implements TelemetryClientConfiguration {
    INSTANCE;

    // Synchronization for instance initialization
    private final Object s_lock = new Object();
    private volatile boolean initialized = false;

    private String instrumentationKey;

    private final ArrayList<ContextInitializer> contextInitializers = new ArrayList<ContextInitializer>();
    private final ArrayList<TelemetryInitializer> telemetryInitializers = new ArrayList<TelemetryInitializer>();

    private TelemetryChannel channel;

    private boolean trackingIsDisabled = true;

    private boolean developerMode = false;

    /**
     * We initialize the instance once by using {@link com.microsoft.applicationinsights.extensibility.TelemetryConfigurationFactory}
     * @return The instance
     */
    public TelemetryConfiguration getActive() {
        if (!initialized) {
            synchronized (s_lock) {
                if (!initialized) {
                    TelemetryConfigurationFactory.INSTANCE.initialize(this);
                }
            }
        }

        return this;
    }

    public TelemetryChannel getChannel() {
        return channel;
    }

    public void setChannel(TelemetryChannel channel) {
        this.channel = channel;
    }

    public boolean isTrackingDisabled() {
        return trackingIsDisabled || Strings.isNullOrEmpty(instrumentationKey);
    }

    public void setTrackingIsDisabled(boolean disable) {
        trackingIsDisabled = disable;
    }

    public boolean isDeveloperMode() {
        return developerMode;
    }

    public void setDeveloperMode(boolean developerMode) {
        this.developerMode = developerMode;
    }

    public List<ContextInitializer> getContextInitializers() {
        return contextInitializers;
    }

    public List<TelemetryInitializer> getTelemetryInitializers() {
        return telemetryInitializers;
    }

    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    public void setInstrumentationKey(String key) {
        // A non null, non empty instrumentation key is a must
        if (Strings.isNullOrEmpty(key)) {
            throw new IllegalArgumentException("key");
        }

        instrumentationKey = key;
    }
}
