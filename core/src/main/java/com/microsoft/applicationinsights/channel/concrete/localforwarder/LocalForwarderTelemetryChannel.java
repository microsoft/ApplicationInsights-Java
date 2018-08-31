package com.microsoft.applicationinsights.channel.concrete.localforwarder;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.channel.concrete.TelemetryChannelBase;
import com.microsoft.applicationinsights.internal.channel.TransmitterFactory;
import com.microsoft.applicationinsights.internal.channel.common.TelemetryBuffer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LimitsEnforcer;
import com.microsoft.applicationinsights.telemetry.BaseTelemetry;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Map;

public class LocalForwarderTelemetryChannel extends TelemetryChannelBase<Telemetry> {

    public static final String ENDPOINT_ENVIRONMENT_VARIABLE_NAME = "APPLICATION_INSIGHTS_LOCAL_FORWARDER_ENDPOINT";
    public static final String ENDPOINT_SYSTEM_PROPERTY_NAME = "applicationinsights.localforwarder.endpoint";

    public LocalForwarderTelemetryChannel(String endpointAddress, boolean developerMode, int maxTelemetryBufferCapacity, int sendIntervalInMillis) {
        super(endpointAddress, developerMode, maxTelemetryBufferCapacity, sendIntervalInMillis);
    }

    public LocalForwarderTelemetryChannel(String endpointAddress, String maxTransmissionStorageCapacity, boolean developerMode, int maxTelemetryBufferCapacity, int sendIntervalInMillis, boolean throttling, int maxInstantRetries) {
        super(endpointAddress, maxTransmissionStorageCapacity, developerMode, maxTelemetryBufferCapacity, sendIntervalInMillis, throttling, maxInstantRetries);
    }

    public LocalForwarderTelemetryChannel(Map<String, String> namesAndValues) {
        super(namesAndValues);
    }

    @Override
    protected synchronized void initialize(String configurationFileEndpoint, String maxTransmissionStorageCapacity, boolean developerMode,
                                           LimitsEnforcer maxTelemetryBufferCapacityEnforcer, LimitsEnforcer sendIntervalInSeconds, boolean throttling, int maxInstantRetry) {
        // using the same policy as TelemetryConfigurationFactory, in priority order: System Property, Environment Variable, Configuration File
        String endpoint = System.getProperty(ENDPOINT_SYSTEM_PROPERTY_NAME, System.getenv(ENDPOINT_ENVIRONMENT_VARIABLE_NAME));
        if (Strings.isNullOrEmpty(endpoint)) {
            endpoint = configurationFileEndpoint;
        }
        super.initialize(endpoint, maxTransmissionStorageCapacity, developerMode, maxTelemetryBufferCapacityEnforcer, sendIntervalInSeconds, throttling, maxInstantRetry);
    }

    @Override
    protected boolean doSend(com.microsoft.applicationinsights.telemetry.Telemetry telemetry) {
        if (telemetryBuffer == null) {
            InternalLogger.INSTANCE.error("Cannot send telemetry. telemetryBuffer is null");
            return false;
        }
        BaseTelemetry base = (BaseTelemetry) telemetry;
        if (base == null) {
            InternalLogger.INSTANCE.warn("Received null telemetry item. Skipping...");
        }

        try {
            Telemetry toSend = LocalForwarderModelTransformer.transform(base);
            if (toSend == null) {
                InternalLogger.INSTANCE.error("Could not find transformer for type='%s'", base.getBaseTypeName());
                return false;
            }
            telemetryBuffer.add(toSend);
            telemetry.reset();
            return true;
        } catch (java.lang.Exception e) {
            InternalLogger.INSTANCE.error("Failed to transform telemetry: %s%nException: %s", telemetry.toString(), ExceptionUtils.getStackTrace(e));
        }
        return false;
    }

    @VisibleForTesting
    TelemetryBuffer<Telemetry> getTelemetryBuffer() {
        return telemetryBuffer;
    }

    @VisibleForTesting
    void setTelemetryBuffer(TelemetryBuffer<Telemetry> buffer) {
        this.telemetryBuffer = buffer;
    }

    @VisibleForTesting
    LocalForwarderTelemetriesTransmitter getTransmitter() {
        return (LocalForwarderTelemetriesTransmitter) telemetriesTransmitter;
    }

    @VisibleForTesting
    void setTransmitter(LocalForwarderTelemetriesTransmitter transmitter) {
        this.telemetriesTransmitter = transmitter;
    }

    @Override
    protected TransmitterFactory<Telemetry> createTransmitterFactory() {
        return new LocalForwarderTelemetryTransmitterFactory();
    }

    @Override
    protected void makeSureEndpointAddressIsValid(String endpointAddress) {
        if (Strings.isNullOrEmpty(endpointAddress)) { // it will fail somewhere downstream
            InternalLogger.INSTANCE.error("No endpoint address given for channel '%s'", this.getClass().getCanonicalName());
            throw new IllegalArgumentException("endpointAddress must be non-empty");
        }
    }
}
