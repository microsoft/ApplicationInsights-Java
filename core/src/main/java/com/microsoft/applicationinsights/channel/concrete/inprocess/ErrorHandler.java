package com.microsoft.applicationinsights.channel.concrete.inprocess;

import org.apache.http.HttpStatus;

import com.microsoft.applicationinsights.internal.channel.TransmissionHandler;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicyManager;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

public class ErrorHandler implements TransmissionHandler {

	private TransmissionPolicyManager transmissionPolicyManager;
	public ErrorHandler(TransmissionPolicyManager policy)
	{
		this.transmissionPolicyManager = policy;
	}
	
	@Override
	public void onTransmissionSent(TransmissionHandlerArgs args) {
		
		validateTransmissionAndSend(args);
	}

	public boolean validateTransmissionAndSend(TransmissionHandlerArgs args) {
		if (args.getTransmission() != null && args.getTransmissionDispatcher() != null)
		{
			args.getTransmission().incrementNumberOfSends();
			switch (args.getResponseCode())
			{
			case HttpStatus.SC_REQUEST_TIMEOUT: 
			case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			case HttpStatus.SC_SERVICE_UNAVAILABLE:
				backoffAndSendTransmission(args);
				return true;
			default:
				InternalLogger.INSTANCE.trace("Http response code %s not handled by %s", args.getResponseCode(), this.getClass().getName());
				return false;
			}             
		} else if (args.getException() != null) {
			backoffAndSendTransmission(args);
			return true;
		}
		return false;
	}

	private void backoffAndSendTransmission(TransmissionHandlerArgs args) {
		this.transmissionPolicyManager.backoff();
		args.getTransmissionDispatcher().dispatch(args.getTransmission());
	}
}
