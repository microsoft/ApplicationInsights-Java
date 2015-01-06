package com.microsoft.applicationinsights.channel;

/**
 * A temporary factory (until we use the configuration) to hook the entities needed by {@link com.microsoft.applicationinsights.channel.TelemetriesTransmitter}
 *
 * Created by gupele on 12/21/2014.
 */
public class NoConfigurationTransmitterFactory implements TransmitterFactory {
    @Override
    public TelemetriesTransmitter create(String endpoint) {
        // An active object with the network sender
        TransmissionNetworkOutput actualNetworkSender = TransmissionNetworkOutput.create(endpoint);
        TransmissionOutput networkSender = new ActiveTransmissionNetworkOutput(actualNetworkSender);

        // An active object with the file system sender
        TransmissionFileSystemOutput fileSystemSender = new TransmissionFileSystemOutput();
        TransmissionOutput activeFileSystemOutput = new ActiveTransmissionFileSystemOutput(fileSystemSender);

        // The dispatcher works with the two active senders
        TransmissionDispatcher dispatcher = new NonBlockingDispatcher(new TransmissionOutput[] {networkSender, activeFileSystemOutput});
        actualNetworkSender.setTransmissionDispatcher(dispatcher);

        // The loader works with the file system loader as the active one does
        TransmissionsLoader transmissionsLoader = new ActiveTransmissionLoader(fileSystemSender, dispatcher);

        // The Transmitter manage all
        TelemetriesTransmitter telemetriesTransmitter = new TransmitterImpl(dispatcher, new GzipTelemetrySerializer(), transmissionsLoader);

        return telemetriesTransmitter;
    }
}
