package com.microsoft.applicationinsights.internal.channel.common;

import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.channel.common.ErrorHandler;
import com.microsoft.applicationinsights.internal.channel.common.ThrottlingHandler;
import com.microsoft.applicationinsights.internal.channel.common.Transmission;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicyManager;

public class ThrottlingHandlerTest {

	private final static String RESPONSE_THROTTLING_HEADER = "Retry-After";
	
	private boolean generateTransmissionWithStatusCode(int code) {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(code);
		args.setTransmission(new Transmission(new byte[] { 0 }, "testcontent", "testencoding"));
		args.setTransmissionDispatcher(mockedDispatcher);
		ThrottlingHandler eh = new ThrottlingHandler(tpm);
		boolean result = eh.validateTransmissionAndSend(args);
		return result;
	}
	
	private boolean generateTransmissionWithStatusCodeAndHeader(int code, String retryHeader) {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(code);
		args.setTransmission(new Transmission(new byte[] { 0 }, "testcontent", "testencoding"));
		args.setTransmissionDispatcher(mockedDispatcher);
		args.setRetryHeader(new BasicHeader(RESPONSE_THROTTLING_HEADER, retryHeader));
		ThrottlingHandler eh = new ThrottlingHandler(tpm);
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
	public void fail408Status() {
		boolean result = generateTransmissionWithStatusCode(408);
		Assert.assertFalse(result);
	}
	
	@Test
	public void pass500Status() {
		boolean result = generateTransmissionWithStatusCode(500);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail503Status() {
		boolean result = generateTransmissionWithStatusCode(503);
		Assert.assertFalse(result);
	}
	
	@Test
	public void failException() {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(0);
		args.setTransmission(null);
		args.setTransmissionDispatcher(mockedDispatcher);
		args.setException(new Exception("Mocked"));
		ThrottlingHandler eh = new ThrottlingHandler(tpm);
		boolean result = eh.validateTransmissionAndSend(args);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail429StatusNoRetryHeader() {
		boolean result = generateTransmissionWithStatusCode(429);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail439StatusNoRetryHeader() {
		boolean result = generateTransmissionWithStatusCode(439);
		Assert.assertFalse(result);
	}
	
	@Test
	public void pass429StatusBadValue() {
		boolean result = generateTransmissionWithStatusCodeAndHeader(429, "3600");
		Assert.assertTrue(result);
	}
	
	@Test
	public void pass429StatusGoodValue() {
		boolean result = generateTransmissionWithStatusCodeAndHeader(429, "Sun, 11 Feb 2018 16:51:18 GMT");
		Assert.assertTrue(result);
	}
	
	@Test
	public void pass439StatusBadValue() {
		boolean result = generateTransmissionWithStatusCodeAndHeader(439, "3600");
		Assert.assertTrue(result);
	}
	
	@Test
	public void pass439StatusGoodValue() {
		boolean result = generateTransmissionWithStatusCodeAndHeader(439, "Sun, 11 Feb 2018 16:51:18 GMT");
		Assert.assertTrue(result);
	}

}
