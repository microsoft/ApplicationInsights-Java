package com.microsoft.applicationinsights.channel.concrete.inprocess;


import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.channel.common.Transmission;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicyManager;


public class ErrorHandlerTest {
	

	private boolean generateTransmissionWithStatusCode(int code) {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(code);
		args.setTransmission(new Transmission(new byte[] { 0 }, "testcontent", "testencoding"));
		args.setTransmissionDispatcher(mockedDispatcher);
		ErrorHandler eh = new ErrorHandler(tpm);
		boolean result = eh.validateTransmissionAndSend(args);
		return result;
	}
	
	@Test
	public void failOnNull() {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		ErrorHandler eh = new ErrorHandler(tpm);
		boolean result = eh.validateTransmissionAndSend(args);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail200Status() {
		boolean result = generateTransmissionWithStatusCode(200);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail206Status() {
		boolean result = generateTransmissionWithStatusCode(206);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail400Status() {
		boolean result = generateTransmissionWithStatusCode(400);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail404Status() {
		boolean result = generateTransmissionWithStatusCode(404);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail429Status() {
		boolean result = generateTransmissionWithStatusCode(429);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail439Status() {
		boolean result = generateTransmissionWithStatusCode(439);
		Assert.assertFalse(result);
	}
	
	@Test
	public void pass408Status() {
		boolean result = generateTransmissionWithStatusCode(408);
		Assert.assertTrue(result);
	}
	
	@Test
	public void pass500Status() {
		boolean result = generateTransmissionWithStatusCode(500);
		Assert.assertTrue(result);
	}
	
	@Test
	public void pass503Status() {
		boolean result = generateTransmissionWithStatusCode(503);
		Assert.assertTrue(result);
	}
	
	@Test
	public void passException() {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(0);
		args.setTransmission(null);
		args.setTransmissionDispatcher(mockedDispatcher);
		args.setException(new Exception("Mocked"));
		ErrorHandler eh = new ErrorHandler(tpm);
		boolean result = eh.validateTransmissionAndSend(args);
		Assert.assertTrue(result);
	}

}
