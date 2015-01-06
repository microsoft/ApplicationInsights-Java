package com.microsoft.applicationinsights.extensibility;

import com.microsoft.applicationinsights.channel.TelemetryChannel;

import java.util.List;

/**
 * Created by gupele on 12/31/2014.
 */
public interface TelemetryClientConfiguration {
    void setEndpoint(String endpoint);

    String getEndpoint();

    void setChannel(TelemetryChannel channel);

    void setTrackingIsDisabled(boolean disable);

    void setDeveloperMode(boolean developerMode);

    boolean isDeveloperMode();

    void setInstrumentationKey(String key);

    List<ContextInitializer> getContextInitializers();

    List<TelemetryInitializer> getTelemetryInitializers();
}
