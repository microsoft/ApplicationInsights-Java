package com.microsoft.applicationinsights.internal.channel;

import com.microsoft.applicationinsights.TelemetryConfiguration;

public interface ConfiguredTransmissionOutput extends TransmissionOutput {
    void setConfiguration(TelemetryConfiguration configuration);
}
