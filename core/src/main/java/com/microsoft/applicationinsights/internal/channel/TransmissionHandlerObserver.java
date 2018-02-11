package com.microsoft.applicationinsights.internal.channel;

public interface TransmissionHandlerObserver extends TransmissionHandler {
	public void addTransmissionHandler(TransmissionHandler handler);
}
