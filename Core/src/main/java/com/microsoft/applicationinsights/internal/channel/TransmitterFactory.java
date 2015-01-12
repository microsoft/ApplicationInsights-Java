package com.microsoft.applicationinsights.internal.channel;

/**
 * Created by gupele on 12/21/2014.
 */
public interface TransmitterFactory {

    TelemetriesTransmitter create(String endpoint);

}
