package com.microsoft.applicationinsights.extensibility;

import com.microsoft.applicationinsights.channel.TelemetryChannel;

import java.util.List;

/**
 * Created by gupele on 12/31/2014.
 */
interface TelemetryClientConfiguration {
    void setChannel(TelemetryChannel channel);

    void setTrackingIsDisabled(boolean disable);

    void setDeveloperMode(boolean developerMode);

    void setInstrumentationKey(String key);

    List<ContextInitializer> getContextInitializers();

    List<TelemetryInitializer> getTelemetryInitializers();
}
