package com.microsoft.applicationinsights.channel;

import com.microsoft.applicationinsights.extensibility.TelemetryConfiguration;

import javax.sound.midi.Transmitter;

/**
 * A temporary factory (until we use the configuration) to hook the entities needed by {@link com.microsoft.applicationinsights.channel.TelemetriesTransmitter}
 *
 * Created by gupele on 12/21/2014.
 */
public class NoConfigurationTransmitterFactory implements TransmitterFactory {
    @Override
    public TelemetriesTransmitter create(TelemetryConfiguration telemetryConfiguration) {
        TransmissionNetworkOutput actualNetworkSender = new TransmissionNetworkOutput();
        TransmissionOutput networkSender = new ActiveTransmissionNetworkOutput(actualNetworkSender);

        TransmissionOutput fileSystemSender = new TransmissionFileSystemOutput();

        TransmissionDispatcher dispatcher = new NonBlockingDispatcher(new TransmissionOutput[] {networkSender, fileSystemSender});
        actualNetworkSender.setTransmissionDispatcher(dispatcher);

        TelemetriesTransmitter telemetriesTransmitter = new TransmitterImpl(dispatcher, new GzipTelemetrySerializer());

        return telemetriesTransmitter;
    }
}
