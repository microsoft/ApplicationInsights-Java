package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.channel.ConfiguredTransmitterFactory;
import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.atomic.AtomicInteger;

public final class LocalForwarderTelemetryTransmitterFactory implements ConfiguredTransmitterFactory<Telemetry> {
    private static final AtomicInteger INSTANCE_ID_POOL = new AtomicInteger(0);

    @Override
    public TelemetriesTransmitter<Telemetry> create(String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled, int maxInstantRetries) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(endpoint), "a non-empty endpoint must be specified");
        return new LocalForwarderTelemetriesTransmitter(ManagedChannelBuilder.forTarget(endpoint).usePlaintext().enableRetry(), true, INSTANCE_ID_POOL.getAndIncrement());
    }

    @Override
    public TelemetriesTransmitter<Telemetry> create(TelemetryConfiguration configuration, String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled, int maxInstantRetries) {
        String theEndpoint = endpoint;
        if (configuration.getConnectionString() != null) {
            theEndpoint = configuration.getEndpointProvider().getIngestionEndpoint().toString();
        }
        Preconditions.checkArgument(!Strings.isNullOrEmpty(theEndpoint), "You must specify an endpoint for LocalForwarder."); // TODO link to doc
        return create(theEndpoint, maxTransmissionStorageCapacity, throttlingIsEnabled, maxInstantRetries);
    }
}
