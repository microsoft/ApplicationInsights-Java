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
		
		if (args.getTransmission() != null && args.getTransmissionDispatcher() != null)
		{
			args.getTransmission().incrementNumberOfSends();
			switch (args.getResponseCode())
			{
			case HttpStatus.SC_REQUEST_TIMEOUT: 
			case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			case HttpStatus.SC_SERVICE_UNAVAILABLE:
			case HttpStatus.SC_BAD_GATEWAY:       // (502) Not called out in spec, still can cause error
			case HttpStatus.SC_GATEWAY_TIMEOUT:   // (504) Not called out in spec, still can cause error
			case HttpStatus.SC_NOT_FOUND:         // (404) Not called out in spec, still can cause error
			case HttpStatus.SC_MOVED_TEMPORARILY: // (302) Not called out in spec, still can cause error
				this.transmissionPolicyManager.backoff();
				args.getTransmissionDispatcher().dispatch(args.getTransmission());
				break;
			default:
				InternalLogger.INSTANCE.trace("Http response code %s not handled by %s", args.getResponseCode(), this.getClass().getName());
				break;
			}             
		} else if (args.getException() != null) {
			this.transmissionPolicyManager.backoff();
			args.getTransmissionDispatcher().dispatch(args.getTransmission());
		}
	}
}
