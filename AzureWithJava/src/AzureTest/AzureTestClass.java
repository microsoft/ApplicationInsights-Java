package AzureTest;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.queue.*;
import java.net.*;
import java.util.*;

import org.apache.commons.lang3.time.StopWatch;
import org.json.*;

public class AzureTestClass {
	
	private final static String storageConnectionString = "AzureStorageConnectionString...";
	private final static String queueName = "export-queue";
	private final static String userAgent = "Pragmatically";
	private final static int secondsToPoll = 60;
	private final static int secondsToSleep = 5;
	private final static int numberOfMessagesToRetrieve = 32;
	private final static int millisecondsInSecond = 1000;

	/**
	 * Sends GET requests to server and expects to get similar result from app insights
	 * @throws Exception
	 */
	public static void main() throws Exception {

		clearQueue();

		ArrayList<ITelemetryItem> expectedTelemetry = new ArrayList<ITelemetryItem>() {{
			add(sendGet(new URI("http://aijavatest-L01:8080/Harel/books?id=Thriller")));
			add(sendGet(new URI("http://aijavatest-L01:8080/Harel/loan?title=Gone%20Girl&id=030758836x&subject=Thriller")));
		}};

		ArrayList<ITelemetryItem> realTelemetry = getTelemetryFromAzure(DocumentType.Requests);

		if (!isExpectedTelemetryExist(expectedTelemetry, realTelemetry)) {
			throw new Exception("Tests are not match.");
		}
	}

	/**
	 * Clears the Azure queue from all existing messages
	 * @throws Exception
	 */
	public static void clearQueue() throws Exception {
		CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
		CloudQueueClient queueClient = account.createCloudQueueClient();
		CloudQueue queue = queueClient.getQueueReference(queueName);

		queue.downloadAttributes();
		System.out.println("Before clearing: Queue contains " + queue.getApproximateMessageCount() + " items");
		System.out.println("Clearing queue ...");
		queue.clear();

		queue.downloadAttributes();
		System.out.println("After clearing: Queue contains " + queue.getApproximateMessageCount() + " items");
	}

	/**
	 * Sends HTTP GET request and returns the expected telemetry from AppInsights
	 * @param uri The URI for the request
	 * @return The expected telemetry items from AppInsights
	 * @throws Exception
	 */
	private static ITelemetryItem sendGet(URI uri) throws Exception {
		HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

		// optional default is GET
		connection.setRequestMethod("GET");

		//add request header
		connection.setRequestProperty("User-Agent", userAgent);

		Integer responseCode = connection.getResponseCode();
		System.out.println("Sending 'GET' request to URL : " + uri.toString());
		System.out.println("Response Code : " + responseCode);

		// Create expected result
		TelemetryItem expectedResult = new TelemetryItem(DocumentType.Requests);
		expectedResult.setProperty("port", Integer.toString(uri.getPort()));
		expectedResult.setProperty("responseCode", responseCode.toString());
		expectedResult.setProperty("uri", uri.toString());
		expectedResult.setProperty("userAgent", userAgent);

		String[] params = uri.getQuery().split("&");
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			expectedResult.setProperty("queryParameter." + name, value);
		}

		return expectedResult;
	}

	/**
	 * Connects to Azure export and retrieves the telemetry items 
	 * @param docType The document type to retrieve from Azure
	 * @return The retrieved telemetry items
	 * @throws Exception
	 */
	public static ArrayList<ITelemetryItem> getTelemetryFromAzure(DocumentType docType) throws Exception {
		CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
		CloudBlobClient blobClient = account.createCloudBlobClient();
		
		CloudQueueClient queueClient = account.createCloudQueueClient();
		CloudQueue queue = queueClient.getQueueReference(queueName);
		
		ArrayList<JSONObject> telemetryAsJson = new ArrayList<JSONObject>();
		StopWatch sw = new StopWatch();

		System.out.println("Starting to poll the queue for " + secondsToPoll + "seconds ...");
		sw.start();

		while(sw.getTime() < secondsToPoll * millisecondsInSecond) {
			System.out.println(sw.getTime() / millisecondsInSecond + " seconds passed. Got " + telemetryAsJson.size() + " items so far.");
			
			ArrayList<CloudQueueMessage> messages = (ArrayList<CloudQueueMessage>) queue.retrieveMessages(numberOfMessagesToRetrieve, secondsToPoll, null, null);
			ArrayList<String> blobUris = getBlobUrisFromQueueMessages(docType, messages);
			for(String blobUri : blobUris) {
				CloudBlockBlob blob = new CloudBlockBlob(new URI(blobUri), blobClient);
				ArrayList<JSONObject> jsonsFromBlobContent = getJsonsFromString(blob.downloadText());
				telemetryAsJson.addAll(jsonsFromBlobContent);
			}
			
			if(messages.size() < numberOfMessagesToRetrieve){
				Helpers.sleep(secondsToSleep);
			}
		}
		
		ArrayList<ITelemetryItem> telemetryItems = new ArrayList<ITelemetryItem>();
		for (JSONObject json : telemetryAsJson) {
			telemetryItems.add(getTelemetryItemFromJson(docType, json));
		}
		
		return telemetryItems;
	}

	/**
	 * Gets a list of blob URIs from Azure queue messages
	 * @param docType The document type to retrieve from Azure
	 * @param messages The Azure queue messages
	 * @return An ArrayList of blob URIs
	 * @throws Exception
	 */
	public static ArrayList<String> getBlobUrisFromQueueMessages(DocumentType docType, ArrayList<CloudQueueMessage> messages) throws Exception {
		ArrayList<String> blobUris = new ArrayList<String>();
		
		for (CloudQueueMessage message : messages) {
			JSONObject messageContentAsJson = new JSONObject(message.getMessageContentAsString());
			String msgDocType = messageContentAsJson.getString("DocumentType");
			
			if(msgDocType.equals(docType.toString())) {
				blobUris.add(messageContentAsJson.getString("BlobUri"));
			}
		}
				
		return blobUris;
	}

	/**
	 * Converts a string to multiple JSON objects
	 * @param jString The string to convert
	 * @return ArrayList of JSON objects
	 */
	public static ArrayList<JSONObject> getJsonsFromString(String jString) {
		String[] jsonStrings = jString.split("\n");
	        ArrayList<JSONObject> jsonObjects = new ArrayList<JSONObject>();

	        for(String s : jsonStrings) {
	        	jsonObjects.add(new JSONObject(s));
	        }
	        
	        return jsonObjects;
	}

	/**
	 * Converts JSON object to TelemetryItem by it's document type
	 * @param docType The document type of the telemetry
	 * @param json The JSON object
	 * @return A TelemetryItem
	 * @throws Exception
	 */
	public static ITelemetryItem getTelemetryItemFromJson(DocumentType docType, JSONObject json) throws Exception {
		if (docType == DocumentType.Requests) {
			return getRequestTelemetryItemFromJson(json);
		}
		
		if (docType == DocumentType.PerformanceCounters) {
			return getPerformanceCounterTelemetryItemFromJson(json);
		}
		
		throw new Exception("Can't find document type: " + docType.toString());
	}

	/**
	 * Converts JSON object to Request TelemetryItem
	 * @param json The JSON object
	 * @return A TelemetryItem
	 */
	public static ITelemetryItem getRequestTelemetryItemFromJson(JSONObject json) {
		String address       = json.getJSONArray("request").getJSONObject(0).getString("url");
		Integer port         = json.getJSONArray("request").getJSONObject(0).getJSONObject("urlData").getInt("port");
		Integer responseCode = json.getJSONArray("request").getJSONObject(0).getInt("responseCode");
		String userAgent     = json.getJSONObject("context").getJSONObject("device").getString("userAgent");

		JSONArray parameters = json.getJSONArray("request").getJSONObject(0).getJSONObject("urlData").getJSONArray("queryParameters");
		
		TelemetryItem telemetryResult = new TelemetryItem(DocumentType.Requests);
		telemetryResult.setProperty("uri", address);
		telemetryResult.setProperty("port", port.toString());
		telemetryResult.setProperty("responseCode", responseCode.toString());
		telemetryResult.setProperty("userAgent", userAgent);
			
		for (int i = 0; i < parameters.length(); ++i) {
			String name  = parameters.getJSONObject(i).getString("parameter");
			String value = parameters.getJSONObject(i).getString("value");
			telemetryResult.setProperty("queryParameter." + name, value);
		}

		return telemetryResult;
	}

	/**
	 * Converts JSON object to PerformanceCounter TelemetryItem
	 * @param json The JSON object
	 * @return A TelemetryItem
	 */
	public static ITelemetryItem getPerformanceCounterTelemetryItemFromJson(JSONObject json) {
		String category = json.getJSONArray("performanceCounter").getJSONObject(0).getString("categoryName");
		String instance = json.getJSONArray("performanceCounter").getJSONObject(0).getString("instanceName");
		
		TelemetryItem telemetryResult = new TelemetryItem(DocumentType.PerformanceCounters);
		telemetryResult.setProperty("category", category);
		telemetryResult.setProperty("instance", instance);

		return telemetryResult;
	}

	/**
	 * Tests if the expected telemetry exists in the real telemetry from AppInsights
	 * @param expectedTelemetryItems The expected telemetry
	 * @param realTelemetryItems The real telemetry
	 * @return true if expected telemetry exists in the real telemetry, otherwise false.
	 * @throws Exception
	 */
	public static boolean isExpectedTelemetryExist(ArrayList<ITelemetryItem> expectedTelemetryItems, 
		                                           ArrayList<ITelemetryItem> realTelemetryItems) throws Exception {
		for (ITelemetryItem expectedItem : expectedTelemetryItems) {
			if (!isExpectedTelemetryItemExist(expectedItem, realTelemetryItems)) {
				// throw new Exception("expected telemetry with document type '" + expectedItem.getDocType().toString() + "' does not exist.");
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Tests if a single expected telemetry item exists in the real telemetry from AppInsights
	 * @param expectedTelemetryItem The expected telemetry item
	 * @param realTelemetryItems The real telemetry
	 * @return true if expected telemetry exists in the real telemetry, otherwise false.
	 */
	public static boolean isExpectedTelemetryItemExist(ITelemetryItem expectedTelemetryItem, ArrayList<ITelemetryItem> realTelemetryItems) {
		for (ITelemetryItem realItem : realTelemetryItems) {
			if(realItem == null) {
				return false;
			}
			
			if (realItem.equalsByProperties(expectedTelemetryItem)) {
				return true;
			}
		}
		
		return false;
	}
}
