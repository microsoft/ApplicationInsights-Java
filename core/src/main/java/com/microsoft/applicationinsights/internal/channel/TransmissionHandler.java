package com.microsoft.applicationinsights.internal.channel;

public interface TransmissionHandler {
	public void onTransmissionSent(TransmissionHandlerArgs args);
}
