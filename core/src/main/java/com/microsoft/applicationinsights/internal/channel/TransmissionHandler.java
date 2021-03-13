package com.microsoft.applicationinsights.internal.channel;

/**
 * This is used to implement classes like {@link com.microsoft.applicationinsights.internal.channel.common.ErrorHandler}
 * and {@link com.microsoft.applicationinsights.internal.channel.common.PartialSuccessHandler}.
 * @author jamdavi
 */
public interface TransmissionHandler {
    /**
     * Called when a transmission is sent by the {@link TransmissionOutputSync}.
     * @param args The {@link TransmissionHandlerArgs} for this handler.
     */
    void onTransmissionSent(TransmissionHandlerArgs args);
}
