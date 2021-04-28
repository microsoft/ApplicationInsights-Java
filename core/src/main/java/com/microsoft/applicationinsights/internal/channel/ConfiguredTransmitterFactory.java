package com.microsoft.applicationinsights.internal.channel;

import com.microsoft.applicationinsights.TelemetryConfiguration;

import javax.annotation.Nullable;

public interface ConfiguredTransmitterFactory<T> {
    /**
     * Either {@code configuration} or {@code endpoint} could be null, but one must be non-null.
     * @param configuration The configuration for the current TelemetryClient
     * @param maxTransmissionStorageCapacity
     * @param throttlingIsEnabled
     * @param maxInstantRetries
     * @return
     */
    TelemetriesTransmitter<T> create(@Nullable TelemetryConfiguration configuration, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled, int maxInstantRetries, boolean isStatsbeat);
}
