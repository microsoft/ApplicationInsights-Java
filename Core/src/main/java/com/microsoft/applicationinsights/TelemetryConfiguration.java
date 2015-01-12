package com.microsoft.applicationinsights;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.applicationinsights.channel.TelemetryChannel;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;

/**
 * Configuration data and logic for telemetry clients.
 */
public final class TelemetryConfiguration implements TelemetryClientConfiguration {

    // Synchronization for instance initialization
    private final static Object s_lock = new Object();
    private static volatile boolean initialized = false;
    private static TelemetryConfiguration active;

    private String instrumentationKey;

    private String endpoint;

    private final ArrayList<ContextInitializer> contextInitializers = new ArrayList<ContextInitializer>();
    private final ArrayList<TelemetryInitializer> telemetryInitializers = new ArrayList<TelemetryInitializer>();

    private TelemetryChannel channel;

    private boolean trackingIsDisabled = false;

    private boolean developerMode = false;

    /**
     * We initialize the instance once by using {@link com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory}
     * @return The instance
     */
    public static TelemetryConfiguration getActive() {
        if (!initialized) {
            synchronized (s_lock) {
                if (!initialized) {
                    active = new TelemetryConfiguration();
                    TelemetryConfigurationFactory.INSTANCE.initialize(active);
                    initialized = true;
                }
            }
        }

        return active;
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
        return trackingIsDisabled;
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
