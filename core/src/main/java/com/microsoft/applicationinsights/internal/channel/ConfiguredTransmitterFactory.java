package com.microsoft.applicationinsights.internal.channel;

import com.microsoft.applicationinsights.TelemetryConfiguration;

public interface ConfiguredTransmitterFactory<T> extends TransmitterFactory<T> {
    TelemetriesTransmitter<T> create(TelemetryConfiguration configuration, String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled, int maxInstantRetries);
}
