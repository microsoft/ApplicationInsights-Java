package com.microsoft.applicationinsights.channel.concrete.inprocess;

import org.apache.http.HttpStatus;

import com.microsoft.applicationinsights.internal.channel.TransmissionHandler;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicyManager;

public class ErrorHandler implements TransmissionHandler {

	private TransmissionPolicyManager transmissionPolicyManager;
	public ErrorHandler(TransmissionPolicyManager policy)
	{
		this.transmissionPolicyManager = policy;
	}
	
	@Override
	public void onTransmissionSent(TransmissionHandlerArgs args) {
		
		if (args.getTransmission() != null && args.getTransmissionDispatcher() != null)
		{
			args.getTransmission().incrementNumberOfSends();
			switch (args.getResponseCode())
			{
			case HttpStatus.SC_REQUEST_TIMEOUT:
			case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			case HttpStatus.SC_BAD_GATEWAY:
			case HttpStatus.SC_GATEWAY_TIMEOUT:
			case HttpStatus.SC_SERVICE_UNAVAILABLE:
				this.transmissionPolicyManager.backoff();
				args.getTransmissionDispatcher().dispatch(args.getTransmission());
				break;
			}             
		} else if (args.getException() != null) {
			this.transmissionPolicyManager.backoff();
			args.getTransmissionDispatcher().dispatch(args.getTransmission());
		}
	}
}
