package com.microsoft.applicationinsights.internal.channel;

import org.apache.http.Header;

import com.microsoft.applicationinsights.internal.channel.common.Transmission;

/**
 * This class is used to store information between the transmission sender and the transmission handlers
 * <p>
 * An example class that uses this are {@link ErrorHandler}
 * @author jamdavi
 *
 */
public class TransmissionHandlerArgs {
	private String responseBody;
	/**
	 * Set the response body.
	 * @param body The HTTP Response from the sender
	 */
	public void setResponseBody(String body) { this.responseBody = body;}
	/**
	 * Get the response body
	 * @return The HTTP Response from the sender
	 */
	public String getResponseBody() { return this.responseBody;}
	
	
	private TransmissionDispatcher transmissionDispatcher;
	/**
	 * Set the {@link TransmissionDispatcher} used by the sender
	 * @param dispatcher The {@link TransmissionDispatcher} used by the sender 
	 */
	public void setTransmissionDispatcher(TransmissionDispatcher dispatcher) { this.transmissionDispatcher = dispatcher;}
	/**
	 * Get the {@link TransmissionDispatcher} used by the sender
	 * @return The {@link TransmissionDispatcher} used by the sender 
	 */
	public TransmissionDispatcher getTransmissionDispatcher() { return this.transmissionDispatcher;}
	
	private Transmission transmission;
	/**
	 * Set the transmission that needs to be passed to the handler.
	 * @param transmission The transmission that needs to be passed to the handler.
	 */
	public void setTransmission(Transmission transmission) { this.transmission = transmission;}
	/**
	 * Get the transmission that needs to be passed to the handler.
	 * @return The transmission used by the handler.
	 */
	public Transmission getTransmission() { return this.transmission;}
	
	private int responseCode;
	/**
	 * Set the response code to be passed to the handler. 
	 * @param code The HTTP response code.
	 */
	public void setResponseCode(int code) { this.responseCode = code;}
	/**
	 * Get the response code for the handler to use. 
	 * @return The HTTP response code.
	 */
	public int getResponseCode() { return this.responseCode;}
	
	private Throwable exception;
	/**
	 * Set the exception thrown by the sender to be passed the handler.
	 * @param ex The exception
	 */
	public void setException(Throwable ex) { this.exception = ex;}
	/**
	 * Get the exception thrown by the sender to be used by the handler.
	 * @return The exception
	 */
	public Throwable getException() { return this.exception;}
	
	private Header retryHeader;
	/**
	 * Set the Retry-After header to be passed to the handler.
	 * @param head The Retry-After header 
	 */
	public void setRetryHeader(Header head) { this.retryHeader = head;}
	/**
	 * Get the Retry-After header to be passed to the handler.
	 * @return The Retry-After header
	 */
	public Header getRetryHeader() { return this.retryHeader;}
}
