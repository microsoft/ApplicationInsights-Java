package com.microsoft.applicationinsights.channel;

import com.microsoft.applicationinsights.extensibility.TelemetryConfiguration;

import javax.sound.midi.Transmitter;

/**
 * Created by gupele on 12/21/2014.
 */
public interface TransmitterFactory {

    TelemetriesTransmitter create(TelemetryConfiguration telemetryConfiguration);

}
