package com.microsoft.applicationinsights;

import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;

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
