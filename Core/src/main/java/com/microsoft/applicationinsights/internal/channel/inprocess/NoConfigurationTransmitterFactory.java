package com.microsoft.applicationinsights.internal.channel.inprocess;

import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
import com.microsoft.applicationinsights.internal.channel.TransmitterFactory;
import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionsLoader;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput;
import com.microsoft.applicationinsights.internal.channel.common.ActiveTransmissionNetworkOutput;
import com.microsoft.applicationinsights.internal.channel.common.ActiveTransmissionFileSystemOutput;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionFileSystemOutput;
import com.microsoft.applicationinsights.internal.channel.common.NonBlockingDispatcher;
import com.microsoft.applicationinsights.internal.channel.common.ActiveTransmissionLoader;
import com.microsoft.applicationinsights.internal.channel.common.TransmitterImpl;
import com.microsoft.applicationinsights.internal.channel.common.GzipTelemetrySerializer;

/**
 * A temporary factory (until we use the configuration) to hook the entities needed by {@link TelemetriesTransmitter}
 *
 * Created by gupele on 12/21/2014.
 */
class NoConfigurationTransmitterFactory implements TransmitterFactory {
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
