package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.TransmitterFactory;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;

public final class LocalForwarderTelemetryTransmitterFactory implements TransmitterFactory<Telemetry> {

    @Override
    public TelemetriesTransmitter<Telemetry> create(String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled, int maxInstantRetries) {
        return new LocalForwarderTelemetriesTransmitter(endpoint);
    }

}
