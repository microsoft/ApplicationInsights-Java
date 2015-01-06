package com.microsoft.applicationinsights.extensibility;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.applicationinsights.channel.TelemetryChannel;

import com.google.common.base.Strings;

/**
 * Configuration data and logic for telemetry clients.
 */
public enum TelemetryConfiguration implements TelemetryClientConfiguration {
    INSTANCE;

    // Synchronization for instance initialization
    private final Object s_lock = new Object();
    private volatile boolean initialized = false;

    private String instrumentationKey;

    private String endpoint;

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

    @Override
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    public TelemetryChannel getChannel() {
        return channel;
    }

    @Override
    public void setChannel(TelemetryChannel channel) {
        this.channel = channel;
    }

    public boolean isTrackingDisabled() {
        return trackingIsDisabled || Strings.isNullOrEmpty(instrumentationKey);
    }

    @Override
    public void setTrackingIsDisabled(boolean disable) {
        trackingIsDisabled = disable;
    }

    @Override
    public boolean isDeveloperMode() {
        return developerMode;
    }

    @Override
    public void setDeveloperMode(boolean developerMode) {
        this.developerMode = developerMode;
    }

    @Override
    public List<ContextInitializer> getContextInitializers() {
        return contextInitializers;
    }

    @Override
    public List<TelemetryInitializer> getTelemetryInitializers() {
        return telemetryInitializers;
    }

    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    @Override
    public void setInstrumentationKey(String key) {
        // A non null, non empty instrumentation key is a must
        if (Strings.isNullOrEmpty(key)) {
            throw new IllegalArgumentException("key");
        }

        instrumentationKey = key;
    }
}
