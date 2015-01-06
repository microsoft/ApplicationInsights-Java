package com.microsoft.applicationinsights.channel;

/**
 * Created by gupele on 12/21/2014.
 */
interface TransmitterFactory {

    TelemetriesTransmitter create(String endpoint);

}
