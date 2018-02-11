package com.microsoft.applicationinsights.channel.concrete.inprocess;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionHandlerArgs;
import com.microsoft.applicationinsights.internal.channel.common.Transmission;
import com.microsoft.applicationinsights.internal.channel.common.TransmissionPolicyManager;

public class PartialSuccessHandlerTest {

	private final byte[] fourItems = new byte[] {31, -117, 8, 0, 0, 0, 0, 0, 0, 0, -19, -110, 77, 79, 2, 49, 16, -122, -17, 38, -2, 7, -45, -13, -74, 105, -69, -80, -127, -67, -111, -32, 1, 13, 23, 69, 60, 15, -69, 3, 84, 119, -37, 77, 91, 49, -124, -16, -33, 109, 23, 69, 49, -98, -12, 32, -121, -67, -50, -41, 59, -13, -50, -77, 35, 27, -76, 36, 23, 9, -47, 80, 35, -55, -55, 84, 21, -42, 56, -77, -12, 108, -44, 52, -107, 42, -64, 43, -93, 39, -38, -87, -43, -38, 59, -74, -56, -122, -112, 2, -49, 80, -10, -95, 39, -5, 11, 16, -48, -21, -31, 112, -71, -52, -106, 34, -19, 15, 36, -69, -34, -96, -10, 36, 33, 94, -75, -45, 36, 23, 3, -54, 37, 21, 98, 38, -78, -100, -53, 60, -51, -104, -112, -100, -14, 62, -25, -95, -54, 65, -35, 84, 120, 7, 62, -44, 10, -50, 25, 79, -120, -70, -59, 109, 104, -4, 16, -94, 81, -119, 70, 41, 26, -75, -24, 87, -79, 40, 3, 43, 71, -14, 29, 1, -59, -108, -10, 104, 53, 84, -52, -107, -49, 115, -76, 46, -84, 29, -26, 60, -63, 6, 114, -55, -62, 104, -70, 64, 15, -44, 105, 104, -36, -38, -60, 21, 67, 79, -119, 27, 85, 32, 83, 101, -88, -68, 25, 77, -57, -93, -7, -124, 78, 123, 3, -50, -87, 96, 22, -53, -38, -24, -110, 21, -58, 54, -84, 62, -70, 82, -104, -6, -92, -73, 50, 5, 84, -15, 84, -44, -12, -31, -2, -112, 58, -82, -94, 77, -119, -17, -66, -2, 114, -68, 9, -25, -111, 71, -91, 75, -13, -22, -82, 4, 63, -55, 89, 83, 97, -8, -116, 7, 93, -4, 73, -31, -45, -83, -17, 66, 14, 93, -52, 28, 12, -118, -65, -28, 82, 8, -111, 113, -103, -90, 100, -97, -112, 18, 60, 68, -9, 23, -32, 112, -74, 109, -30, 18, -19, -1, -57, 49, -98, -76, -31, -15, 123, 73, 75, -103, 60, 82, 54, 67, -25, -37, -46, 40, -44, 88, -45, -96, -11, 10, -61, -83, -6, -91, -86, -10, -5, -3, -27, -59, -18, 31, -64, 76, 69, 7, 102, 7, -26, 1, 76, -47, -127, -39, -127, 121, -114, 96, -54, -77, 2, 83, 118, 96, 118, 96, 30, -64, 76, 127, 6, -13, 13, -51, 46, -90, -77, 98, 10, 0, 0} ;
	private final String fourItemsNonGZIP = "{\"ver\":1,\"name\":\"Microsoft.ApplicationInsights.b69a3a06e25a425ba1a44e9ff6f13582.Event\",\"time\":\"2018-02-11T16:02:36.120-0500\",\"sampleRate\":100.0,\"iKey\":\"b69a3a06-e25a-425b-a1a4-4e9ff6f13582\",\"tags\":{\"ai.internal.sdkVersion\":\"java:2.0.0-beta-snapshot\",\"ai.device.id\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.locale\":\"en-US\",\"ai.internal.nodename\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.os\":\"Windows 10\",\"ai.device.roleInstance\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.osVersion\":\"Windows 10\",\"ai.session.id\":\"20180211160233\"},\"data\":{\"baseType\":\"EventData\",\"baseData\":{\"ver\":2,\"name\":\"TestEvent0\",\"properties\":null}}}\r\n" + 
			"{\"ver\":1,\"name\":\"Microsoft.ApplicationInsights.b69a3a06e25a425ba1a44e9ff6f13582.Event\",\"time\":\"2018-02-11T16:02:36.131-0500\",\"sampleRate\":100.0,\"iKey\":\"b69a3a06-e25a-425b-a1a4-4e9ff6f13582\",\"tags\":{\"ai.internal.sdkVersion\":\"java:2.0.0-beta-snapshot\",\"ai.device.id\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.locale\":\"en-US\",\"ai.internal.nodename\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.os\":\"Windows 10\",\"ai.device.roleInstance\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.osVersion\":\"Windows 10\",\"ai.session.id\":\"20180211160233\"},\"data\":{\"baseType\":\"EventData\",\"baseData\":{\"ver\":2,\"name\":\"TestEvent1\",\"properties\":null}}}\r\n" + 
			"{\"ver\":1,\"name\":\"Microsoft.ApplicationInsights.b69a3a06e25a425ba1a44e9ff6f13582.Event\",\"time\":\"2018-02-11T16:02:36.131-0500\",\"sampleRate\":100.0,\"iKey\":\"b69a3a06-e25a-425b-a1a4-4e9ff6f13582\",\"tags\":{\"ai.internal.sdkVersion\":\"java:2.0.0-beta-snapshot\",\"ai.device.id\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.locale\":\"en-US\",\"ai.internal.nodename\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.os\":\"Windows 10\",\"ai.device.roleInstance\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.osVersion\":\"Windows 10\",\"ai.session.id\":\"20180211160233\"},\"data\":{\"baseType\":\"EventData\",\"baseData\":{\"ver\":2,\"name\":\"TestEvent2\",\"properties\":null}}}\r\n" + 
			"{\"ver\":1,\"name\":\"Microsoft.ApplicationInsights.b69a3a06e25a425ba1a44e9ff6f13582.Event\",\"time\":\"2018-02-11T16:02:36.132-0500\",\"sampleRate\":100.0,\"iKey\":\"b69a3a06-e25a-425b-a1a4-4e9ff6f13582\",\"tags\":{\"ai.internal.sdkVersion\":\"java:2.0.0-beta-snapshot\",\"ai.device.id\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.locale\":\"en-US\",\"ai.internal.nodename\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.os\":\"Windows 10\",\"ai.device.roleInstance\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.osVersion\":\"Windows 10\",\"ai.session.id\":\"20180211160233\"},\"data\":{\"baseType\":\"EventData\",\"baseData\":{\"ver\":2,\"name\":\"TestEvent3\",\"properties\":null}}}";

	private boolean generateTransmissionWithStatusCode(int code) {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(code);
		args.setTransmission(new Transmission(new byte[] { 0 }, "testcontent", "testencoding"));
		args.setTransmissionDispatcher(mockedDispatcher);
		PartialSuccessHandler eh = new PartialSuccessHandler(tpm);
		boolean result = eh.validateTransmissionAndSend(args);
		return result;
	}
	
	private boolean generateTransmissionWithPartialResult(String responseBody) {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(206);
		args.setTransmission(new Transmission(fourItems, "application/x-json-stream", "gzip"));
		args.setTransmissionDispatcher(mockedDispatcher);
		args.setResponseBody(responseBody);
		PartialSuccessHandler eh = new PartialSuccessHandler(tpm);
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
	public void fail500Status() {
		boolean result = generateTransmissionWithStatusCode(500);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail503Status() {
		boolean result = generateTransmissionWithStatusCode(503);
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
	public void failEmptyArrayList() {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(206);
		args.setTransmission(new Transmission(fourItems, "application/x-json-stream", "gzip"));
		args.setTransmissionDispatcher(mockedDispatcher);
		PartialSuccessHandler eh = new PartialSuccessHandler(tpm);
		boolean result = eh.sendNewTransmission(args, new ArrayList<String>());
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail206StatusSkipAcceptedRecieved() {
		String validResult = "{\r\n" + 
				"    \"itemsReceived\": 4,\r\n" + 
				"    \"itemsAccepted\": 4,\r\n" + 
				"    \"errors\": []\r\n }";
		boolean result = generateTransmissionWithPartialResult(validResult);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail206StatusSkipRecievedLargerThanSent() {
		String validResult = "{\r\n" + 
				"    \"itemsReceived\": 10,\r\n" + 
				"    \"itemsAccepted\": 9,\r\n" + 
				"    \"errors\": [{\r\n" + 
				"            \"index\": 0,\r\n" + 
				"            \"statusCode\": 400,\r\n" + 
				"            \"message\": \"109: Field 'startTime' on type 'RequestData' is required but missing or empty. Expected: string, Actual: undefined\"\r\n" + 
				"        }]\r\n }";
		boolean result = generateTransmissionWithPartialResult(validResult);
		Assert.assertFalse(result);
	}
	
	@Test
	public void fail206IndexOutOfRange() {
		String validResult = "{\r\n" + 
				"    \"itemsReceived\": 4,\r\n" + 
				"    \"itemsAccepted\": 1,\r\n" + 
				"    \"errors\": [\r\n" + 
				"        {\r\n" + 
				"            \"index\": 20,\r\n" + 
				"            \"statusCode\": 400,\r\n" + 
				"            \"message\": \"109: Field 'startTime' on type 'RequestData' is required but missing or empty. Expected: string, Actual: undefined\"\r\n" + 
				"        },\r\n" + 
				"	 {\r\n" + 
				"            \"index\": 12,\r\n" + 
				"            \"statusCode\": 500,\r\n" + 
				"            \"message\": \"Internal Server Error\"\r\n" + 
				"        },\r\n" + 
				"	 {\r\n" + 
				"            \"index\": 22,\r\n" + 
				"            \"statusCode\": 439,\r\n" + 
				"            \"message\": \"Too many requests\"\r\n" + 
				"        }\r\n" + 
				"    ]\r\n" + 
				"}";
		boolean result = generateTransmissionWithPartialResult(validResult);
		Assert.assertFalse(result);
	}
	
	@Test
	public void pass206MixedIndexOutOfRange() {
		String validResult = "{\r\n" + 
				"    \"itemsReceived\": 4,\r\n" + 
				"    \"itemsAccepted\": 1,\r\n" + 
				"    \"errors\": [\r\n" + 
				"        {\r\n" + 
				"            \"index\": 5,\r\n" + 
				"            \"statusCode\": 400,\r\n" + 
				"            \"message\": \"109: Field 'startTime' on type 'RequestData' is required but missing or empty. Expected: string, Actual: undefined\"\r\n" + 
				"        },\r\n" + 
				"	 {\r\n" + 
				"            \"index\": 1,\r\n" + 
				"            \"statusCode\": 500,\r\n" + 
				"            \"message\": \"Internal Server Error\"\r\n" + 
				"        },\r\n" + 
				"	 {\r\n" + 
				"            \"index\": 22,\r\n" + 
				"            \"statusCode\": 439,\r\n" + 
				"            \"message\": \"Too many requests\"\r\n" + 
				"        }\r\n" + 
				"    ]\r\n" + 
				"}";
		boolean result = generateTransmissionWithPartialResult(validResult);
		Assert.assertTrue(result);
	}
	
	@Test
	public void pass206Status() {
		String validResult = "{\r\n" + 
				"    \"itemsReceived\": 4,\r\n" + 
				"    \"itemsAccepted\": 1,\r\n" + 
				"    \"errors\": [\r\n" + 
				"        {\r\n" + 
				"            \"index\": 0,\r\n" + 
				"            \"statusCode\": 400,\r\n" + 
				"            \"message\": \"109: Field 'startTime' on type 'RequestData' is required but missing or empty. Expected: string, Actual: undefined\"\r\n" + 
				"        },\r\n" + 
				"	 {\r\n" + 
				"            \"index\": 1,\r\n" + 
				"            \"statusCode\": 500,\r\n" + 
				"            \"message\": \"Internal Server Error\"\r\n" + 
				"        },\r\n" + 
				"	 {\r\n" + 
				"            \"index\": 2,\r\n" + 
				"            \"statusCode\": 439,\r\n" + 
				"            \"message\": \"Too many requests\"\r\n" + 
				"        }\r\n" + 
				"    ]\r\n" + 
				"}";
		boolean result = generateTransmissionWithPartialResult(validResult);
		Assert.assertTrue(result);
	}
	
	@Test
	public void passSingleItemArrayList() {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(206);
		args.setTransmission(new Transmission(fourItems, "application/x-json-stream", "gzip"));
		args.setTransmissionDispatcher(mockedDispatcher);
		PartialSuccessHandler eh = new PartialSuccessHandler(tpm);
		ArrayList<String> singleItem = new ArrayList<String>();
		singleItem.add("{\"ver\":1,\"name\":\"Microsoft.ApplicationInsights.b69a3a06e25a425ba1a44e9ff6f13582.Event\",\"time\":\"2018-02-11T16:02:36.120-0500\",\"sampleRate\":100.0,\"iKey\":\"b69a3a06-e25a-425b-a1a4-4e9ff6f13582\",\"tags\":{\"ai.internal.sdkVersion\":\"java:2.0.0-beta-snapshot\",\"ai.device.id\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.locale\":\"en-US\",\"ai.internal.nodename\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.os\":\"Windows 10\",\"ai.device.roleInstance\":\"JAMDAVI-M4800-1.redmond.corp.microsoft.com\",\"ai.device.osVersion\":\"Windows 10\",\"ai.session.id\":\"20180211160233\"},\"data\":{\"baseType\":\"EventData\",\"baseData\":{\"ver\":2,\"name\":\"TestEvent0\",\"properties\":null}}}\r\n"); 
		boolean result = eh.sendNewTransmission(args, singleItem);
		Assert.assertTrue(result);
	}
	
	@Test
	public void passGenerateOriginalItemsGZIP() {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(206);
		args.setTransmission(new Transmission(fourItems, "application/x-json-stream", "gzip"));
		args.setTransmissionDispatcher(mockedDispatcher);
		PartialSuccessHandler eh = new PartialSuccessHandler(tpm); 
		List<String> originalItems = eh.generateOriginalItems(args);
		Assert.assertEquals(4, originalItems.size());
	}
	
	@Test
	public void passGenerateOriginalItemsNonGZIP() {
		TransmissionPolicyManager tpm = new TransmissionPolicyManager(true);
		TransmissionDispatcher mockedDispatcher = Mockito.mock(TransmissionDispatcher.class);
		TransmissionHandlerArgs args = new TransmissionHandlerArgs();
		args.setResponseCode(206);
		args.setTransmission(new Transmission(fourItemsNonGZIP.getBytes(), "application/json", "utf8"));
		args.setTransmissionDispatcher(mockedDispatcher);
		PartialSuccessHandler eh = new PartialSuccessHandler(tpm); 
		List<String> originalItems = eh.generateOriginalItems(args);
		Assert.assertEquals(4, originalItems.size());
	}

}
