package com.microsoft.applicationinsights.smoketest;

import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;

import org.junit.*;

import static org.junit.Assert.*;

public class SimpleTraceDataTest extends AiSmokeTest {

	@Test
	public void doCalcSendsRequestDataAndTraceData() throws Exception {
		System.out.println("Wait for app to finish deploying...");
		String appContext = warFileName.replace(".war", "");
		String baseUrl = "http://localhost:" + appServerPort + "/" + appContext;
		waitForUrl(baseUrl, 120, TimeUnit.SECONDS, appContext);
		System.out.println("Test app health check complete.");

		String url = baseUrl+"/doCalc?leftOperand=1&rightOperand=2&operator=plus";
		String content = HttpHelper.get(url);

		assertNotNull(content);
		assertTrue(content.length() > 0);
		
		System.out.println("Waiting 10s for telemetry...");
		TimeUnit.SECONDS.sleep(10);
		System.out.println("Finished waiting for telemetry. Starting validation...");

		assertTrue("mocked ingestion has no data", mockedIngestion.hasData());
		assertTrue("mocked ingestion has 0 items", mockedIngestion.getItemCount() > 0);
		
		assertEquals(2, mockedIngestion.getCountForType("RequestData"));
		assertEquals(3, mockedIngestion.getCountForType("MessageData"));
		int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 5;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems), expectedItems, totalItems);
		// TODO get trace data envelope and verify value

		MessageData d = GetMessageData(0);
		final String expectedMessage = "This is first trace message.";
		assertEquals(expectedMessage, d.getMessage());

		MessageData d2 = GetMessageData(1);
		final String expectedMessage2 = "This is second trace message.";		
		assertEquals(expectedMessage2, d2.getMessage());
		assertEquals(SeverityLevel.Error, d2.getSeverityLevel());

		MessageData d3 = GetMessageData(2);
		final String expectedMessage3 = "This is third trace message.";
		final String expectedValue = "Test";
		assertEquals(expectedMessage3, d3.getMessage());
		assertEquals(SeverityLevel.Information, d3.getSeverityLevel());
		assertEquals(expectedValue, d3.getProperties().get("key"));
	}

	/**
	 * @param index
	 * @return Message Data
	 */
	private MessageData GetMessageData(int index){
		Envelope mEnvelope = mockedIngestion.getItemsByType("MessageData").get(index);
		Data<MessageData> dHolder = (Data<MessageData>) mEnvelope.getData();
		MessageData d = dHolder.getBaseData();
		return d; 
	}
}