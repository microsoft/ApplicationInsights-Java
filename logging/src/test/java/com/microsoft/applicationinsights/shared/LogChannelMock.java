package com.microsoft.applicationinsights.shared;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * Created by gupele on 1/18/2015.
 */
public final class LogChannelMock implements TelemetryChannel {

    public LogChannelMock(Map<String, String> properties) {
    }

    @Override
    public boolean isDeveloperMode() {
        return false;
    }

    @Override
    public void setDeveloperMode(boolean value) {
    }

    @Override
    public void send(Telemetry item) {
        LogChannelMockVerifier.INSTANCE.add(item);
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
    }
}
