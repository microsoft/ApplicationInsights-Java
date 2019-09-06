package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.channel.ConfiguredTransmitterFactory;
import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

public final class LocalForwarderTelemetryTransmitterFactory implements ConfiguredTransmitterFactory<Telemetry> {
    private static final AtomicInteger INSTANCE_ID_POOL = new AtomicInteger(0);

    /**
     * @deprecated Use {@link #create(TelemetryConfiguration, String, String, boolean, int)}
     */
    @Deprecated
    @Override
    public TelemetriesTransmitter<Telemetry> create(String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled, int maxInstantRetries) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(endpoint), "a non-empty endpoint must be specified");
        return new LocalForwarderTelemetriesTransmitter(ManagedChannelBuilder.forTarget(endpoint).usePlaintext().enableRetry(), true, INSTANCE_ID_POOL.getAndIncrement());
    }

    /**
     *
     * @param configuration The configuration for the current TelemetryClient
     * @param endpoint The endpoint to use. Overrides endpoint set by connection string, if any.
     * @param maxTransmissionStorageCapacity n/a
     * @param throttlingIsEnabled n/a
     * @param maxInstantRetries n/a
     * @return
     */
    @Override
    public TelemetriesTransmitter<Telemetry> create(@Nullable TelemetryConfiguration configuration, @Nullable String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled, int maxInstantRetries) {
        String theEndpoint = null;
        if (StringUtils.isNotBlank(endpoint)) {
            theEndpoint = endpoint;
        } else if (configuration != null && configuration.getConnectionString() != null) {
            theEndpoint = configuration.getEndpointProvider().getIngestionEndpoint().toString();
        }
        Preconditions.checkArgument(!Strings.isNullOrEmpty(theEndpoint), "You must specify an endpoint for LocalForwarder.");
        if (InternalLogger.INSTANCE.isTraceEnabled()) {
            InternalLogger.INSTANCE.trace("LocalForwarder using endpoint %s", theEndpoint);
        }
        return new LocalForwarderTelemetriesTransmitter(ManagedChannelBuilder.forTarget(endpoint).usePlaintext().enableRetry(), true, INSTANCE_ID_POOL.getAndIncrement());
    }
}
