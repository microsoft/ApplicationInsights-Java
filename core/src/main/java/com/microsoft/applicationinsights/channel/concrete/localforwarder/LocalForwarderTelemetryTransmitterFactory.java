package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.TransmitterFactory;

public class LocalForwarderTelemetryTransmitterFactory implements TransmitterFactory {
    @Override
    public TelemetriesTransmitter create(String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled) {
        return null;
    }

    @Override
    public TelemetriesTransmitter create(String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled, int maxInstantRetries) {
        return null;
    }
}
