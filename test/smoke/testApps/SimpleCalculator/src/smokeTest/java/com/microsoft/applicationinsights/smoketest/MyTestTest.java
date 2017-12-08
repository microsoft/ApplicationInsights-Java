package com.microsoft.applicationinsights.smoketest;

import java.util.concurrent.TimeUnit;
import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;

import org.junit.*;

import static org.junit.Assert.*;

public class MyTestTest extends AiSmokeTest {

	@Test
	public void doCalcSendsRequestDataAndMetricData() throws Exception {
		System.out.println("Wait for app to finish deploying...");
		String appContext = warFileName.replace(".war", "");
		String baseUrl = "http://localhost:" + appServerPort + "/" + appContext;
		waitForUrl(baseUrl, 60, TimeUnit.SECONDS, appContext);
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
		assertEquals(1, mockedIngestion.getCountForType("MetricData"));
		int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 3;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems), expectedItems, totalItems);
		// TODO get metric data envelope and verify value

		Envelope mEnvelope = mockedIngestion.getItemsByType("MetricData").get(0);
		Data<MetricData> dHolder = (Data<MetricData>) mEnvelope.getData();
		MetricData d = dHolder.getBaseData();
		List<DataPoint> metrics = d.getMetrics();
		assertEquals(1, metrics.size());
		DataPoint dp = metrics.get(0);
		
		final double expectedValue = 111222333.0;
		final double epsilon = Math.ulp(expectedValue);
		assertEquals(DataPointType.Measurement, dp.getKind());
		assertEquals(expectedValue, dp.getValue(), epsilon);
		assertEquals("TimeToRespond", dp.getName());
		assertEquals(Integer.valueOf(1),  dp.getCount());

		assertNull(dp.getMin());
		assertNull(dp.getMax());
		assertNull(dp.getStdDev());
	}
}