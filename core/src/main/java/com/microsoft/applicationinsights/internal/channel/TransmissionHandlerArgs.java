package com.microsoft.applicationinsights.internal.channel;

import org.apache.http.Header;

import com.microsoft.applicationinsights.internal.channel.common.Transmission;

public class TransmissionHandlerArgs {
	private String responseBody;
	public void setResponseBody(String body) { this.responseBody = body;}
	public String getResponseBody() { return this.responseBody;}
	
	
	private TransmissionDispatcher transmissionDispatcher;
	public void setTransmissionDispatcher(TransmissionDispatcher dispatcher) { this.transmissionDispatcher = dispatcher;}
	public TransmissionDispatcher getTransmissionDispatcher() { return this.transmissionDispatcher;}
	
	private Transmission transmission;
	public void setTransmission(Transmission transmission) { this.transmission = transmission;}
	public Transmission getTransmission() { return this.transmission;}
	
	private int responseCode;
	public void setResponseCode(int code) { this.responseCode = code;}
	public int getResponseCode() { return this.responseCode;}
	
	private Throwable exception;
	public void setException(Throwable ex) { this.exception = ex;}
	public Throwable getException() { return this.exception;}
	
	private Header retryHeader;
	public void setRetryHeader(Header head) { this.retryHeader = head;}
	public Header getRetryHeader() { return this.retryHeader;}
}
