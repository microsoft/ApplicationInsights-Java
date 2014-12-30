package com.microsoft.applicationinsights.extensibility;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.channel.TelemetryChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration data and logic for telemetry clients.
 */
public class TelemetryConfiguration {

    private static TelemetryConfiguration s_active;
    private static final Object s_lock = new Object();

    private String instrumentationKey;

    private final List<ContextInitializer> contextInitializers = new ArrayList<ContextInitializer>();
    private final List<TelemetryInitializer> telemetryInitializers = new ArrayList<TelemetryInitializer>();
    private TelemetryChannel channel;
    private boolean trackingIsDisabled = true;
    private boolean developerMode = false;

    public static TelemetryConfiguration getActive() {
        if (s_active == null) {
            synchronized (s_lock) {
                s_active = new TelemetryConfiguration();
                s_active.trackingIsDisabled = false;
                TelemetryConfigurationFactory.INSTANCE.Initialize(s_active);
            }
        }

        return s_active;
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
        if (Strings.isNullOrEmpty(key)) {
            throw new IllegalArgumentException("key");
        }
        instrumentationKey = key;
    }
}
