package com.microsoft.applicationinsights.internal.channel;

/**
 * An interface that is used to create a concrete class that is called by the the {@link
 * TransmissionHandlerObserver}
 *
 * <p>This is used to implement classes like {@link
 * com.microsoft.applicationinsights.internal.channel.common.ErrorHandler} and {@link
 * com.microsoft.applicationinsights.internal.channel.common.PartialSuccessHandler}.
 *
 * @author jamdavi
 */
public interface TransmissionHandler {
  /**
   * Called when a transmission is sent by the {@link TransmissionOutput}.
   *
   * @param args The {@link TransmissionHandlerArgs} for this handler.
   */
  void onTransmissionSent(TransmissionHandlerArgs args);
}
