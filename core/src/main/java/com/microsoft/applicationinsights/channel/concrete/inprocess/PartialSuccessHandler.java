package com.microsoft.applicationinsights.channel.concrete.inprocess;

import org.apache.http.HttpStatus;

import com.microsoft.applicationinsights.internal.channel.TransmissionHandler;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicyManager;

public class PartialSuccessHandler implements TransmissionHandler {

	
	public PartialSuccessHandler(TransmissionPolicyManager policy)
	{

	}
	
	@Override
	public void onTransmissionSent(TransmissionHandlerArgs args) {
		if (args.getTransmission() != null && args.getTransmissionDispatcher() != null)
		{
			args.getTransmission().incrementNumberOfSends();
			switch (args.getResponseCode())
			{
			case HttpStatus.SC_PARTIAL_CONTENT:

				args.getTransmissionDispatcher().dispatch(args.getTransmission());
				break;
			}             
		}
	}
}
	
	
