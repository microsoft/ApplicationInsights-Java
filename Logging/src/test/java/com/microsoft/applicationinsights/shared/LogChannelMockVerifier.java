package com.microsoft.applicationinsights.shared;

import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gupele on 1/18/2015.
 */
public enum LogChannelMockVerifier {
    INSTANCE;

    private ArrayList<Telemetry> telemetryCollection = new ArrayList<Telemetry>();

    public void add(Telemetry item) {
        telemetryCollection.add(item);
    }

    public List<Telemetry> getTelemetryCollection() {
        return telemetryCollection;
    }
}
