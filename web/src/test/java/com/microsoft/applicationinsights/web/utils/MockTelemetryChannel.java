package com.microsoft.applicationinsights.web.utils;

import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by yonisha on 2/2/2015.
 */
public class MockTelemetryChannel  implements TelemetryChannel {

    List<Telemetry> telemetryItems = new ArrayList<Telemetry>();

    public List<Telemetry> getTelemetryItems() {
        return telemetryItems;
    }

    @Override
    public boolean isDeveloperMode() {
        return true;
    }

    @Override
    public void setDeveloperMode(boolean value) {

    }

    @Override
    public void send(Telemetry item) {
        telemetryItems.add(item);
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {

    }
}